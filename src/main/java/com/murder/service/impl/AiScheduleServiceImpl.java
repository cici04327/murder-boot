package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.dto.AiDmAssignDTO;
import com.murder.dto.AiDmConfirmDTO;
import com.murder.entity.*;
import com.murder.mapper.*;
import com.murder.service.AiScheduleService;
import com.murder.vo.AiDmSuggestVO;
import com.murder.vo.AiDmSuggestVO.DmCandidate;
import com.murder.vo.AiDmSuggestVO.ScheduleSuggest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScheduleServiceImpl implements AiScheduleService {

    private final ScriptScheduleMapper scriptScheduleMapper;
    private final DmMapper dmMapper;
    private final AiScheduleTaskMapper aiScheduleTaskMapper;
    private final ScriptMapper scriptMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.api-url:https://api.deepseek.com/chat/completions}")
    private String aiApiUrl;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:deepseek-chat}")
    private String aiModel;

    // ==================== DM Assignment ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiDmSuggestVO suggestDmAssignment(AiDmAssignDTO dto) {
        Long storeId = resolveAuthorizedStoreId(dto.getStoreId());
        LocalDate start = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();
        LocalDate end   = dto.getEndDate()   != null ? dto.getEndDate()   : start.plusDays(6);

        AiScheduleTask task = AiScheduleTask.builder()
                .storeId(storeId).taskType("DM_ASSIGN")
                .startDate(start).endDate(end)
                .status("RUNNING").triggerType("MANUAL").build();
        aiScheduleTaskMapper.insert(task);

        try {
            LambdaQueryWrapper<ScriptSchedule> sw = new LambdaQueryWrapper<ScriptSchedule>()
                    .eq(ScriptSchedule::getStoreId, storeId)
                    .between(ScriptSchedule::getScheduleDate, start, end)
                    .eq(ScriptSchedule::getIsDeleted, 0)
                    .orderByAsc(ScriptSchedule::getScheduleDate, ScriptSchedule::getStartTime);
            if (Boolean.TRUE.equals(dto.getOnlyUnassigned())) sw.isNull(ScriptSchedule::getDmId);
            List<ScriptSchedule> schedules = scriptScheduleMapper.selectList(sw);

            List<Dm> dmList = dmMapper.selectList(new LambdaQueryWrapper<Dm>()
                    .eq(Dm::getStoreId, storeId).eq(Dm::getStatus, 1).eq(Dm::getIsDeleted, 0));

            if (schedules.isEmpty() || dmList.isEmpty()) {
                String msg = dmList.isEmpty() ? "门店暂无在职DM，无法自动分配。" : "该时间段内无需分配DM的排期。";
                AiDmSuggestVO vo = AiDmSuggestVO.builder().taskId(task.getId())
                        .totalSchedules(schedules.size()).assignedCount(0)
                        .unassignedCount(schedules.size()).summary(msg).suggests(new ArrayList<>()).build();
                updateTask(task, "DONE", schedules.size(), 0, msg);
                return vo;
            }

            Map<Long, Integer> dmLoad = new HashMap<>();
            for (Dm dm : dmList) {
                int c = scriptScheduleMapper.selectCount(new LambdaQueryWrapper<ScriptSchedule>()
                        .eq(ScriptSchedule::getDmId, dm.getId())
                        .between(ScriptSchedule::getScheduleDate, start, end)
                        .eq(ScriptSchedule::getIsDeleted, 0)).intValue();
                dmLoad.put(dm.getId(), c);
            }

            List<ScheduleSuggest> suggests = new ArrayList<>();
            int assignedCount = 0;

            for (ScriptSchedule schedule : schedules) {
                Script script = scriptMapper.selectById(schedule.getScriptId());
                String tags = script != null ? script.getTags() : "";
                String name = script != null ? script.getName() : "Unknown";

                List<DmCandidate> candidates = new ArrayList<>();
                for (Dm dm : dmList) {
                    if (hasTimeConflict(dm.getId(), schedule)) continue;
                    int score = calcScore(dm, tags, dmLoad.getOrDefault(dm.getId(), 0));
                    candidates.add(DmCandidate.builder()
                            .dmId(dm.getId()).name(dm.getName()).avatar(dm.getAvatar())
                            .rating(dm.getRating() != null ? dm.getRating() : BigDecimal.ZERO)
                            .scheduledCount(dmLoad.getOrDefault(dm.getId(), 0))
                            .matchScore(score).build());
                }
                candidates.sort((a, b) -> b.getMatchScore() - a.getMatchScore());
                List<DmCandidate> top3 = candidates.stream().limit(3).collect(Collectors.toList());
                boolean has = !top3.isEmpty();
                DmCandidate best = has ? top3.get(0) : null;
                if (has) { assignedCount++; dmLoad.merge(best.getDmId(), 1, Integer::sum); }

                suggests.add(ScheduleSuggest.builder()
                        .scheduleId(schedule.getId()).scheduleDate(schedule.getScheduleDate())
                        .startTime(schedule.getStartTime()).endTime(schedule.getEndTime())
                        .scriptName(name).scriptTags(tags)
                        .recommendDmId(has ? best.getDmId() : null)
                        .recommendDmName(has ? best.getName() : null)
                        .recommendDmAvatar(has ? best.getAvatar() : null)
                        .recommendDmRating(has ? best.getRating() : null)
                        .dmScheduledCount(has ? best.getScheduledCount() : 0)
                        .matchScore(has ? best.getMatchScore() : 0)
                        .hasCandidate(has).candidates(top3)
                        .reason(has ? buildReason(best, tags) : "暂无可用DM（时间冲突或无在职DM）")
                        .build());
            }

            String summary = callAiForDmSummary(suggests, dmList, start, end);
            AiDmSuggestVO vo = AiDmSuggestVO.builder().taskId(task.getId())
                    .totalSchedules(schedules.size()).assignedCount(assignedCount)
                    .unassignedCount(schedules.size() - assignedCount)
                    .summary(summary).suggests(suggests).build();
            updateTask(task, "DONE", schedules.size(), assignedCount, summary);
            return vo;
        } catch (Exception e) {
            log.error("AI DM assign failed", e);
            updateTask(task, "FAILED", 0, 0, null);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDmAssignment(AiDmConfirmDTO dto) {
        if (dto.getItems() == null) return;
        Long scopedStoreId = resolveScopedStoreIdForMutation();
        for (AiDmConfirmDTO.AssignItem item : dto.getItems()) {
            validateAssignmentItem(item, scopedStoreId);
            ScriptSchedule s = new ScriptSchedule();
            s.setId(item.getScheduleId());
            s.setDmId(item.getDmId());
            scriptScheduleMapper.updateById(s);
        }
        log.info("DM assignment confirmed, total={}", dto.getItems().size());
    }

    @Override
    public List<?> listTaskHistory(Long storeId, int limit) {
        return aiScheduleTaskMapper.listByStore(resolveAuthorizedStoreId(storeId), limit);
    }

    @Override
    public void autoAssignDmForWeekSchedules() {
        log.info("[AutoTask] Auto assigning DM for next week schedules...");
        List<Long> storeIds = scriptScheduleMapper.selectList(
                new LambdaQueryWrapper<ScriptSchedule>().eq(ScriptSchedule::getIsDeleted, 0).select(ScriptSchedule::getStoreId)
        ).stream().map(ScriptSchedule::getStoreId).distinct().collect(Collectors.toList());

        LocalDate nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        for (Long storeId : storeIds) {
            try {
                AiDmAssignDTO dto = new AiDmAssignDTO();
                dto.setStoreId(storeId);
                dto.setStartDate(nextMonday);
                dto.setEndDate(nextMonday.plusDays(6));
                dto.setOnlyUnassigned(true);
                suggestDmAssignment(dto);
            } catch (Exception e) {
                log.error("Auto DM assign failed for store={}", storeId, e);
            }
        }
    }

    // ==================== Helpers ====================

    private Long resolveAuthorizedStoreId(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if (role == null || role.isBlank()) {
            if (requestedStoreId == null) {
                throw new BaseException("门店不能为空");
            }
            return requestedStoreId;
        }
        if ("SUPER_ADMIN".equals(role)) {
            if (requestedStoreId == null) {
                throw new BaseException("请指定门店");
            }
            return requestedStoreId;
        }
        if ("STORE_ADMIN".equals(role)) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前管理员未绑定门店");
            }
            return storeId;
        }
        throw new BaseException("无权限访问AI排班");
    }

    private Long resolveScopedStoreIdForMutation() {
        String role = BaseContext.getRole();
        if (role == null || role.isBlank() || "SUPER_ADMIN".equals(role)) {
            return null;
        }
        if ("STORE_ADMIN".equals(role)) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前管理员未绑定门店");
            }
            return storeId;
        }
        throw new BaseException("无权限操作AI排班");
    }

    private void validateAssignmentItem(AiDmConfirmDTO.AssignItem item, Long scopedStoreId) {
        if (item == null || item.getScheduleId() == null || item.getDmId() == null) {
            throw new BaseException("分配参数不完整");
        }
        ScriptSchedule schedule = scriptScheduleMapper.selectById(item.getScheduleId());
        if (schedule == null || Integer.valueOf(1).equals(schedule.getIsDeleted())) {
            throw new BaseException("排期不存在或已删除");
        }
        if (scopedStoreId != null && !Objects.equals(scopedStoreId, schedule.getStoreId())) {
            throw new BaseException("无权限操作其他门店排期");
        }
        Dm dm = dmMapper.selectById(item.getDmId());
        if (dm == null || Integer.valueOf(1).equals(dm.getIsDeleted()) || !Integer.valueOf(1).equals(dm.getStatus())) {
            throw new BaseException("DM不存在或不可用");
        }
        if (!Objects.equals(schedule.getStoreId(), dm.getStoreId())) {
            throw new BaseException("DM不属于当前排期所在门店");
        }
    }

    private boolean hasTimeConflict(Long dmId, ScriptSchedule target) {
        int c = scriptScheduleMapper.selectCount(new LambdaQueryWrapper<ScriptSchedule>()
                .eq(ScriptSchedule::getDmId, dmId)
                .eq(ScriptSchedule::getScheduleDate, target.getScheduleDate())
                .ne(ScriptSchedule::getId, target.getId())
                .lt(ScriptSchedule::getStartTime, target.getEndTime())
                .gt(ScriptSchedule::getEndTime, target.getStartTime())
                .eq(ScriptSchedule::getIsDeleted, 0)).intValue();
        return c > 0;
    }

    private int calcScore(Dm dm, String scriptTags, int scheduled) {
        int score = 50;
        if (scriptTags != null && !scriptTags.isEmpty() && dm.getStyleTags() != null) {
            Set<String> dmTagSet = new HashSet<>(Arrays.asList(dm.getStyleTags().split(",")));
            long matched = Arrays.stream(scriptTags.split(",")).filter(t -> dmTagSet.contains(t.trim())).count();
            score += (int) Math.min(30, matched * 10);
        }
        if (dm.getRating() != null) score += (int)(dm.getRating().doubleValue() / 5.0 * 15);
        score += Math.max(0, 10 - scheduled * 2);
        return Math.min(100, Math.max(0, score));
    }

    private String buildReason(DmCandidate dm, String scriptTags) {
        StringBuilder sb = new StringBuilder();
        sb.append(dm.getName()).append(" 综合匹配得分 ").append(dm.getMatchScore()).append(" 分");
        if (scriptTags != null && !scriptTags.isEmpty()) sb.append("，风格与剧本标签匹配");
        sb.append("，评分 ").append(dm.getRating()).append(" 星");
        sb.append("，本期已排 ").append(dm.getScheduledCount()).append(" 场");
        return sb.toString();
    }

    private String callAiForDmSummary(List<ScheduleSuggest> suggests, List<Dm> dmList, LocalDate start, LocalDate end) {
        try {
            int total = suggests.size();
            long assigned = suggests.stream().filter(s -> Boolean.TRUE.equals(s.getHasCandidate())).count();
            String defaultSummary = "规则引擎分配完成。共 " + total + " 场排期，成功推荐 " + assigned + " 场，"
                    + (total - assigned) + " 场暂无可用DM。门店共有 " + dmList.size() + " 名在职DM。";

            if (aiApiKey == null || aiApiKey.isBlank()) return defaultSummary;

            String prompt = "你是一个剧本杀门店的排班助手。请用中文简洁总结以下DM分配结果（2-3句话）：" +
                    "排班周期：" + start + " 至 " + end + "，" +
                    "共 " + total + " 场排期，成功分配 " + assigned + " 场，未能分配 " + (total - assigned) + " 场，" +
                    "门店共 " + dmList.size() + " 名在职DM。请给出专业的分析和建议。";
            String aiResult = callDeepSeek(prompt);
            return aiResult != null ? aiResult : defaultSummary;
        } catch (Exception e) {
            log.warn("AI summary failed, using default", e);
            return "AI分配完成，共推荐 " + suggests.stream().filter(s -> Boolean.TRUE.equals(s.getHasCandidate())).count()
                    + "/" + suggests.size() + " 场。";
        }
    }

    @SuppressWarnings("unchecked")
    private String callDeepSeek(String prompt) {
        if (aiApiKey == null || aiApiKey.isBlank()) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey);
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiModel);
            body.put("temperature", 0.7);
            body.put("max_tokens", 300);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            body.put("messages", messages);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(aiApiUrl, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    if (msg != null) return (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("DeepSeek call failed: {}", e.getMessage());
        }
        return null;
    }

    private void updateTask(AiScheduleTask task, String status, int total, int success, String summary) {
        task.setStatus(status);
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setResultSummary(summary);
        aiScheduleTaskMapper.updateById(task);
    }
}


