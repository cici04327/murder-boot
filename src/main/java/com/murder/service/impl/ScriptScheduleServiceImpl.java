package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Dm;
import com.murder.entity.ScriptSchedule;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ScriptScheduleMapper;
import com.murder.service.ScriptScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 剧本排期服务实现类
 */
@Slf4j
@Service
public class ScriptScheduleServiceImpl implements ScriptScheduleService {

    @Autowired
    private ScriptScheduleMapper scriptScheduleMapper;

    @Autowired(required = false)
    private DmMapper dmMapper;

    @Override
    public List<ScriptSchedule> listByStoreAndDate(Long storeId, LocalDate scheduleDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate);
        fillDmInfo(list);
        return list;
    }

    @Override
    public List<ScriptSchedule> listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDateRange(storeId, startDate, endDate);
        fillDmInfo(list);
        return list;
    }

    /**
     * 批量填充 DM 姓名和头像（非数据库字段）
     */
    private void fillDmInfo(List<ScriptSchedule> list) {
        if (list == null || list.isEmpty() || dmMapper == null) return;
        List<Long> dmIds = list.stream()
                .map(ScriptSchedule::getDmId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (dmIds.isEmpty()) return;
        Map<Long, Dm> dmMap = new HashMap<>();
        dmMapper.selectBatchIds(dmIds).forEach(dm -> dmMap.put(dm.getId(), dm));
        list.forEach(s -> {
            if (s.getDmId() != null && dmMap.containsKey(s.getDmId())) {
                Dm dm = dmMap.get(s.getDmId());
                s.setDmName(dm.getName());
                s.setDmAvatar(dm.getAvatar());
            }
        });
    }

    @Override
    public void add(ScriptSchedule schedule) {
        // 检查时间冲突
        checkTimeConflict(schedule);
        schedule.setCurrentPlayers(0);
        schedule.setStatus(1);
        schedule.setIsDeleted(0);
        scriptScheduleMapper.insert(schedule);
        log.info("新增剧本排期: storeId={}, scriptId={}, date={}", 
                schedule.getStoreId(), schedule.getScriptId(), schedule.getScheduleDate());
    }

    @Override
    @Transactional
    public void batchAdd(List<ScriptSchedule> schedules) {
        for (ScriptSchedule schedule : schedules) {
            add(schedule);
        }
    }

    @Override
    public void update(ScriptSchedule schedule) {
        // 编辑时也检查冲突（排除自身）
        if (schedule.getRoomId() != null && schedule.getScheduleDate() != null
                && schedule.getStartTime() != null && schedule.getEndTime() != null) {
            checkTimeConflictExcludeSelf(schedule);
        }
        scriptScheduleMapper.updateById(schedule);
        log.info("更新剧本排期: id={}", schedule.getId());
    }

    @Override
    public Map<String, Object> checkConflict(Long storeId, Long roomId, String scheduleDate,
                                              String startTime, String endTime, Long excludeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate date = LocalDate.parse(scheduleDate);
            LocalTime start = LocalTime.parse(startTime.length() == 5 ? startTime + ":00" : startTime);
            LocalTime end   = LocalTime.parse(endTime.length()   == 5 ? endTime   + ":00" : endTime);

            LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScriptSchedule::getStoreId, storeId)
                   .eq(ScriptSchedule::getRoomId, roomId)
                   .eq(ScriptSchedule::getScheduleDate, date)
                   .eq(ScriptSchedule::getIsDeleted, 0)
                   .ne(ScriptSchedule::getStatus, 2) // 排除已关闭的排期
                   .and(w -> w
                       .between(ScriptSchedule::getStartTime, start, end)
                       .or().between(ScriptSchedule::getEndTime, start, end)
                       .or().le(ScriptSchedule::getStartTime, start).ge(ScriptSchedule::getEndTime, end)
                   );
            if (excludeId != null) {
                wrapper.ne(ScriptSchedule::getId, excludeId);
            }

            List<ScriptSchedule> conflicts = scriptScheduleMapper.selectList(wrapper);
            if (conflicts.isEmpty()) {
                result.put("conflict", false);
                result.put("message", "时段无冲突，可以新增");
            } else {
                ScriptSchedule c = conflicts.get(0);
                result.put("conflict", true);
                result.put("count", conflicts.size());
                result.put("message", String.format("与已有排期冲突：%s ~ %s（共%d个冲突）",
                        c.getStartTime().toString().substring(0, 5),
                        c.getEndTime().toString().substring(0, 5),
                        conflicts.size()));
                result.put("conflictSchedules", conflicts.stream().map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("startTime", s.getStartTime().toString().substring(0, 5));
                    m.put("endTime", s.getEndTime().toString().substring(0, 5));
                    return m;
                }).collect(java.util.stream.Collectors.toList()));
            }
        } catch (Exception e) {
            log.warn("冲突检测异常: {}", e.getMessage());
            result.put("conflict", false);
            result.put("message", "检测异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public void delete(Long id) {
        scriptScheduleMapper.deleteById(id);
        log.info("删除剧本排期: id={}", id);
    }

    @Override
    public ScriptSchedule getById(Long id) {
        return scriptScheduleMapper.selectById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ScriptSchedule schedule = new ScriptSchedule();
        schedule.setId(id);
        schedule.setStatus(status);
        scriptScheduleMapper.updateById(schedule);
        log.info("更新排期状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional
    public void generateSchedules(Long storeId, Long scriptId, Long roomId,
                                   LocalDate startDate, LocalDate endDate,
                                   List<String> timeSlots, Integer maxPlayers) {
        List<ScriptSchedule> schedules = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            for (String timeSlot : timeSlots) {
                String[] times = timeSlot.split("-");
                if (times.length == 2) {
                    ScriptSchedule schedule = ScriptSchedule.builder()
                            .storeId(storeId)
                            .scriptId(scriptId)
                            .roomId(roomId)
                            .scheduleDate(currentDate)
                            .startTime(LocalTime.parse(times[0].trim()))
                            .endTime(LocalTime.parse(times[1].trim()))
                            .maxPlayers(maxPlayers)
                            .currentPlayers(0)
                            .status(1)
                            .isDeleted(0)
                            .build();
                    schedules.add(schedule);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        batchAdd(schedules);
        log.info("批量生成排期: storeId={}, scriptId={}, 共{}条", storeId, scriptId, schedules.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCurrentPlayers(Long scheduleId, int count) {
        ScriptSchedule schedule = scriptScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("排期不存在，跳过人数更新: scheduleId={}", scheduleId);
            return;
        }
        int newCount = (schedule.getCurrentPlayers() == null ? 0 : schedule.getCurrentPlayers()) + count;
        ScriptSchedule update = new ScriptSchedule();
        update.setId(scheduleId);
        update.setCurrentPlayers(newCount);
        // 若已满则自动关闭排期
        if (schedule.getMaxPlayers() != null && newCount >= schedule.getMaxPlayers()) {
            update.setStatus(0); // 0=已满
        }
        scriptScheduleMapper.updateById(update);
        log.info("排期人数+{}: scheduleId={}, currentPlayers={}", count, scheduleId, newCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCurrentPlayers(Long scheduleId, int count) {
        ScriptSchedule schedule = scriptScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("排期不存在，跳过人数回退: scheduleId={}", scheduleId);
            return;
        }
        int newCount = Math.max(0, (schedule.getCurrentPlayers() == null ? 0 : schedule.getCurrentPlayers()) - count);
        ScriptSchedule update = new ScriptSchedule();
        update.setId(scheduleId);
        update.setCurrentPlayers(newCount);
        // 若之前是已满状态则恢复为可预约
        if (Integer.valueOf(0).equals(schedule.getStatus())) {
            update.setStatus(1);
        }
        scriptScheduleMapper.updateById(update);
        log.info("排期人数-{}: scheduleId={}, currentPlayers={}", count, scheduleId, newCount);
    }

    @Override
    public List<ScriptSchedule> getAvailableSchedules(Long scriptId, Long storeId, Integer days) {
        if (days == null || days <= 0) days = 7;
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days - 1);

        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getIsDeleted, 0)
               .eq(ScriptSchedule::getStatus, 1)           // 状态：可预约
               .ge(ScriptSchedule::getScheduleDate, today)
               .le(ScriptSchedule::getScheduleDate, endDate);

        if (scriptId != null) {
            wrapper.eq(ScriptSchedule::getScriptId, scriptId);
        }
        if (storeId != null) {
            wrapper.eq(ScriptSchedule::getStoreId, storeId);
        }

        wrapper.orderByAsc(ScriptSchedule::getScheduleDate, ScriptSchedule::getStartTime);

        // SQL 级过滤：直接排除 currentPlayers >= maxPlayers 的场次（性能优化）
        wrapper.apply("current_players < max_players");
        List<ScriptSchedule> list = scriptScheduleMapper.selectList(wrapper);
        // 填充 DM 姓名和头像
        fillDmInfo(list);
        return list;
    }

    /**
     * 编辑时检查冲突（排除自身）
     */
    private void checkTimeConflictExcludeSelf(ScriptSchedule schedule) {
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getStoreId, schedule.getStoreId())
               .eq(ScriptSchedule::getRoomId, schedule.getRoomId())
               .eq(ScriptSchedule::getScheduleDate, schedule.getScheduleDate())
               .eq(ScriptSchedule::getIsDeleted, 0)
               .ne(ScriptSchedule::getId, schedule.getId())
               .and(w -> w
                   .between(ScriptSchedule::getStartTime, schedule.getStartTime(), schedule.getEndTime())
                   .or().between(ScriptSchedule::getEndTime, schedule.getStartTime(), schedule.getEndTime())
                   .or().le(ScriptSchedule::getStartTime, schedule.getStartTime())
                       .ge(ScriptSchedule::getEndTime, schedule.getEndTime())
               );
        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该房间在此时段已有排期，请选择其他时间");
        }
    }

    /**
     * 检查时间冲突
     */
    private void checkTimeConflict(ScriptSchedule schedule) {
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getStoreId, schedule.getStoreId())
               .eq(ScriptSchedule::getRoomId, schedule.getRoomId())
               .eq(ScriptSchedule::getScheduleDate, schedule.getScheduleDate())
               .eq(ScriptSchedule::getIsDeleted, 0)
               .and(w -> w
                   .between(ScriptSchedule::getStartTime, schedule.getStartTime(), schedule.getEndTime())
                   .or()
                   .between(ScriptSchedule::getEndTime, schedule.getStartTime(), schedule.getEndTime())
                   .or()
                   .le(ScriptSchedule::getStartTime, schedule.getStartTime())
                   .ge(ScriptSchedule::getEndTime, schedule.getEndTime())
               );
        
        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该房间在此时段已有排期，请选择其他时间");
        }
    }
}
