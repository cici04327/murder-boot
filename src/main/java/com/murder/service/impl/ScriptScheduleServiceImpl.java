package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.entity.Dm;
import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.entity.Script;
import com.murder.entity.ScriptSchedule;
import com.murder.entity.Store;
import com.murder.entity.StoreRoom;
import com.murder.mapper.DmMapper;
import com.murder.mapper.GroupOrderMapper;
import com.murder.mapper.ReservationMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptScheduleMapper;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.StoreRoomMapper;
import com.murder.service.ScriptScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Autowired
    private ScriptMapper scriptMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private StoreRoomMapper storeRoomMapper;

    @Autowired(required = false)
    private ReservationMapper reservationMapper;

    @Autowired(required = false)
    private GroupOrderMapper groupOrderMapper;

    @Override
    public List<ScriptSchedule> listByStoreAndDate(Long storeId, LocalDate scheduleDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate);
        list = applyDmScheduleScope(list);
        fillScheduleDisplayInfo(list);
        return list;
    }

    @Override
    public List<ScriptSchedule> listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDateRange(storeId, startDate, endDate);
        list = applyDmScheduleScope(list);
        fillScheduleDisplayInfo(list);
        return list;
    }

    @Override
    public List<ScriptSchedule> listMySchedulesByDate(Long storeId, LocalDate scheduleDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate);
        list = applyDmScheduleScope(list, true);
        fillScheduleDisplayInfo(list);
        return list;
    }

    @Override
    public List<ScriptSchedule> listMySchedulesByDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<ScriptSchedule> list = scriptScheduleMapper.listByStoreAndDateRange(storeId, startDate, endDate);
        list = applyDmScheduleScope(list, true);
        fillScheduleDisplayInfo(list);
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

    private void fillScheduleDisplayInfo(List<ScriptSchedule> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> scriptIds = list.stream()
                .map(ScriptSchedule::getScriptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> storeIds = list.stream()
                .map(ScriptSchedule::getStoreId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> roomIds = list.stream()
                .map(ScriptSchedule::getRoomId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Script> scriptMap = new HashMap<>();
        Map<Long, Store> storeMap = new HashMap<>();
        Map<Long, StoreRoom> roomMap = new HashMap<>();

        if (!scriptIds.isEmpty()) {
            scriptMapper.selectBatchIds(scriptIds).forEach(script -> scriptMap.put(script.getId(), script));
        }
        if (!storeIds.isEmpty()) {
            storeMapper.selectBatchIds(storeIds).forEach(store -> storeMap.put(store.getId(), store));
        }
        if (!roomIds.isEmpty()) {
            storeRoomMapper.selectBatchIds(roomIds).forEach(room -> roomMap.put(room.getId(), room));
        }

        list.forEach(schedule -> {
            Script script = schedule.getScriptId() != null ? scriptMap.get(schedule.getScriptId()) : null;
            if (script != null) {
                if (!StringUtils.hasText(schedule.getScriptName())) {
                    schedule.setScriptName(script.getName());
                }
                schedule.setCover(script.getCover());
                schedule.setPrice(script.getPrice());
                schedule.setRating(script.getRating());
                schedule.setDuration(script.getDuration());
                schedule.setDifficulty(script.getDifficulty());
                schedule.setPlayerCount(script.getPlayerCount());
            }

            Store store = schedule.getStoreId() != null ? storeMap.get(schedule.getStoreId()) : null;
            if (store != null) {
                schedule.setStoreName(store.getName());
                schedule.setStoreAddress(store.getAddress());
            }

            StoreRoom room = schedule.getRoomId() != null ? roomMap.get(schedule.getRoomId()) : null;
            if (room != null && !StringUtils.hasText(schedule.getRoomName())) {
                schedule.setRoomName(room.getName());
            }
        });

        fillDmInfo(list);
        fillEffectiveCurrentPlayers(list);
    }

    @Override
    public void add(ScriptSchedule schedule) {
        validateDmAssignment(schedule);
        checkTimeConflict(schedule);
        checkDmConflict(schedule);
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
        validateDmAssignment(schedule);
        if (schedule.getRoomId() != null && schedule.getScheduleDate() != null
                && schedule.getStartTime() != null && schedule.getEndTime() != null) {
            checkTimeConflictExcludeSelf(schedule);
            checkDmConflictExcludeSelf(schedule);
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
        ScriptSchedule schedule = scriptScheduleMapper.selectById(id);
        if (schedule == null) {
            return null;
        }
        if (isDmStaff() && !Objects.equals(schedule.getDmId(), BaseContext.getDmId())) {
            throw new SecurityException("没有权限查看该场次");
        }
        fillScheduleDisplayInfo(Collections.singletonList(schedule));
        return schedule;
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
    public Map<String, Object> precheckGenerateSchedules(Long storeId, Long scriptId, Long roomId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         List<String> timeSlots, Integer maxPlayers, Long dmId) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        int totalCount = 0;
        int validCount = 0;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            for (String timeSlot : timeSlots) {
                String[] times = timeSlot.split("-");
                if (times.length != 2) {
                    continue;
                }
                totalCount++;
                LocalTime startTime = LocalTime.parse(times[0].trim());
                LocalTime endTime = LocalTime.parse(times[1].trim());

                List<String> reasons = new ArrayList<>();
                if (hasRoomConflict(storeId, roomId, currentDate, startTime, endTime, null)) {
                    reasons.add("房间冲突");
                }
                if (dmId != null && hasDmConflict(dmId, currentDate, startTime, endTime, null)) {
                    reasons.add("DM冲突");
                }

                if (reasons.isEmpty()) {
                    validCount++;
                    continue;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("scheduleDate", currentDate.toString());
                item.put("startTime", startTime.toString());
                item.put("endTime", endTime.toString());
                item.put("reason", String.join("、", reasons));
                conflicts.add(item);
            }
            currentDate = currentDate.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("validCount", validCount);
        result.put("conflictCount", conflicts.size());
        result.put("hasConflict", !conflicts.isEmpty());
        result.put("conflicts", conflicts);
        result.put("message", conflicts.isEmpty()
                ? "未发现冲突，可直接批量生成"
                : String.format("预计可生成 %d 个，冲突 %d 个", validCount, conflicts.size()));
        return result;
    }

    @Override
    @Transactional
    public void generateSchedules(Long storeId, Long scriptId, Long roomId,
                                   LocalDate startDate, LocalDate endDate,
                                   List<String> timeSlots, Integer maxPlayers, Long dmId) {
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
                            .dmId(dmId)
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
        List<ScriptSchedule> activeList = filterExpiredAvailableSchedules(list);
        fillScheduleDisplayInfo(activeList);
        return activeList;
    }

    private List<ScriptSchedule> filterExpiredAvailableSchedules(List<ScriptSchedule> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ScriptSchedule> activeList = new ArrayList<>(list.size());
        for (ScriptSchedule schedule : list) {
            if (hasScheduleStarted(schedule, now)) {
                closeExpiredSchedule(schedule);
                continue;
            }
            activeList.add(schedule);
        }
        return activeList;
    }

    private boolean hasScheduleStarted(ScriptSchedule schedule, LocalDateTime now) {
        if (schedule == null || schedule.getScheduleDate() == null || schedule.getStartTime() == null) {
            return false;
        }
        LocalDateTime startAt = LocalDateTime.of(schedule.getScheduleDate(), schedule.getStartTime());
        return !startAt.isAfter(now);
    }

    private void closeExpiredSchedule(ScriptSchedule schedule) {
        if (schedule == null || schedule.getId() == null || Integer.valueOf(2).equals(schedule.getStatus())) {
            return;
        }

        String remark = buildExpiredRemark(schedule.getRemark());
        ScriptSchedule update = new ScriptSchedule();
        update.setId(schedule.getId());
        update.setStatus(2);
        update.setRemark(remark);
        scriptScheduleMapper.updateById(update);

        schedule.setStatus(2);
        schedule.setRemark(remark);
        log.info("排期开场后自动关闭: scheduleId={}", schedule.getId());
    }

    private String buildExpiredRemark(String currentRemark) {
        String expiredRemark = "开场时间已过，排期已自动关闭";
        if (!StringUtils.hasText(currentRemark)) {
            return expiredRemark;
        }
        if (currentRemark.contains(expiredRemark)) {
            return currentRemark;
        }
        return currentRemark + "；" + expiredRemark;
    }

    private List<ScriptSchedule> applyDmScheduleScope(List<ScriptSchedule> list) {
        return applyDmScheduleScope(list, false);
    }

    private List<ScriptSchedule> applyDmScheduleScope(List<ScriptSchedule> list, boolean forceDmScope) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        if (!forceDmScope && !isDmStaff()) {
            return list;
        }
        Long dmId = BaseContext.getDmId();
        if (dmId == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(schedule -> Objects.equals(schedule.getDmId(), dmId))
                .collect(Collectors.toList());
    }

    private boolean isDmStaff() {
        return "STORE_STAFF".equals(BaseContext.getRole()) && "DM".equals(BaseContext.getStaffRole());
    }

    /**
     * 编辑时检查冲突（排除自身）
     */
    private void fillEffectiveCurrentPlayers(List<ScriptSchedule> schedules) {
        if (schedules == null || schedules.isEmpty() || reservationMapper == null) {
            return;
        }

        List<Long> scheduleIds = schedules.stream()
                .map(ScriptSchedule::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (scheduleIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<Reservation> reservationWrapper = new LambdaQueryWrapper<>();
        reservationWrapper.in(Reservation::getScheduleId, scheduleIds)
                .eq(Reservation::getIsDeleted, 0)
                .ne(Reservation::getStatus, 4);
        List<Reservation> reservations = reservationMapper.selectList(reservationWrapper);
        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        Map<Long, Integer> standalonePlayersBySchedule = new HashMap<>();
        Map<Long, List<Long>> activeGroupIdsBySchedule = new HashMap<>();
        Map<Long, GroupOrder> activeGroupMap = loadActiveGroupMap(reservations);

        for (Reservation reservation : reservations) {
            Long scheduleId = reservation.getScheduleId();
            if (scheduleId == null) continue;
            Long groupId = reservation.getGroupId();
            GroupOrder activeGroup = groupId != null ? activeGroupMap.get(groupId) : null;
            if (activeGroup != null) {
                activeGroupIdsBySchedule.computeIfAbsent(scheduleId, key -> new ArrayList<>());
                List<Long> ids = activeGroupIdsBySchedule.get(scheduleId);
                if (!ids.contains(groupId)) {
                    ids.add(groupId);
                }
                continue;
            }
            standalonePlayersBySchedule.merge(scheduleId, resolvePlayerCount(reservation.getPlayerCount()), Integer::sum);
        }

        for (ScriptSchedule schedule : schedules) {
            if (schedule.getId() == null) continue;
            int effectivePlayers = standalonePlayersBySchedule.getOrDefault(schedule.getId(), 0);
            List<Long> groupIds = activeGroupIdsBySchedule.getOrDefault(schedule.getId(), Collections.emptyList());
            for (Long groupId : groupIds) {
                GroupOrder group = activeGroupMap.get(groupId);
                if (group != null) {
                    effectivePlayers += resolvePlayerCount(group.getCurrentCount());
                }
            }
            schedule.setCurrentPlayers(Math.max(resolvePlayerCount(schedule.getCurrentPlayers()), effectivePlayers));
        }
    }

    private Map<Long, GroupOrder> loadActiveGroupMap(List<Reservation> reservations) {
        if (groupOrderMapper == null || reservations == null || reservations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> groupIds = reservations.stream()
                .map(Reservation::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDateTime now = LocalDateTime.now();
        Map<Long, GroupOrder> map = new HashMap<>();
        groupOrderMapper.selectBatchIds(groupIds).forEach(group -> {
            if (group != null && isActiveGroupForDisplay(group, now)) {
                map.put(group.getId(), group);
            }
        });
        return map;
    }

    private boolean isActiveGroupForDisplay(GroupOrder group, LocalDateTime now) {
        if (group == null || !Integer.valueOf(1).equals(group.getStatus())) {
            return false;
        }
        if (group.getCreateTime() != null && group.getCreateTime().isBefore(now.minusHours(24))) {
            return false;
        }
        return group.getPlayTime() == null || group.getPlayTime().isAfter(now.plusHours(2));
    }

    private int resolvePlayerCount(Integer count) {
        return count != null && count > 0 ? count : 0;
    }

    private boolean hasRoomConflict(Long storeId, Long roomId, LocalDate scheduleDate,
                                    LocalTime startTime, LocalTime endTime, Long excludeId) {
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getStoreId, storeId)
                .eq(ScriptSchedule::getRoomId, roomId)
                .eq(ScriptSchedule::getScheduleDate, scheduleDate)
                .eq(ScriptSchedule::getIsDeleted, 0)
                .ne(ScriptSchedule::getStatus, 2)
                .and(w -> w
                        .lt(ScriptSchedule::getStartTime, endTime)
                        .gt(ScriptSchedule::getEndTime, startTime)
                );
        if (excludeId != null) {
            wrapper.ne(ScriptSchedule::getId, excludeId);
        }
        return scriptScheduleMapper.selectCount(wrapper) > 0;
    }

    private boolean hasDmConflict(Long dmId, LocalDate scheduleDate,
                                  LocalTime startTime, LocalTime endTime, Long excludeId) {
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getDmId, dmId)
                .eq(ScriptSchedule::getScheduleDate, scheduleDate)
                .eq(ScriptSchedule::getIsDeleted, 0)
                .ne(ScriptSchedule::getStatus, 2)
                .and(w -> w
                        .lt(ScriptSchedule::getStartTime, endTime)
                        .gt(ScriptSchedule::getEndTime, startTime)
                );
        if (excludeId != null) {
            wrapper.ne(ScriptSchedule::getId, excludeId);
        }
        return scriptScheduleMapper.selectCount(wrapper) > 0;
    }

    private void checkTimeConflictExcludeSelf(ScriptSchedule schedule) {
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getStoreId, schedule.getStoreId())
               .eq(ScriptSchedule::getRoomId, schedule.getRoomId())
               .eq(ScriptSchedule::getScheduleDate, schedule.getScheduleDate())
               .eq(ScriptSchedule::getIsDeleted, 0)
               .ne(ScriptSchedule::getId, schedule.getId())
               .and(w -> w
                   .lt(ScriptSchedule::getStartTime, schedule.getEndTime())
                   .gt(ScriptSchedule::getEndTime, schedule.getStartTime())
               );
        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BaseException("该房间在此时段已有排期，请选择其他时间");
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
                   .lt(ScriptSchedule::getStartTime, schedule.getEndTime())
                   .gt(ScriptSchedule::getEndTime, schedule.getStartTime())
               );

        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BaseException("该房间在此时段已有排期，请选择其他时间");
        }
    }

    private void checkDmConflict(ScriptSchedule schedule) {
        if (schedule.getDmId() == null || schedule.getScheduleDate() == null
                || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return;
        }
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getDmId, schedule.getDmId())
               .eq(ScriptSchedule::getScheduleDate, schedule.getScheduleDate())
               .eq(ScriptSchedule::getIsDeleted, 0)
               .ne(ScriptSchedule::getStatus, 2)
               .and(w -> w
                   .lt(ScriptSchedule::getStartTime, schedule.getEndTime())
                   .gt(ScriptSchedule::getEndTime, schedule.getStartTime())
               );
        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BaseException("所选DM在该时段已有排期，请选择其他DM或调整时间");
        }
    }

    private void checkDmConflictExcludeSelf(ScriptSchedule schedule) {
        if (schedule.getDmId() == null || schedule.getScheduleDate() == null
                || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return;
        }
        LambdaQueryWrapper<ScriptSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptSchedule::getDmId, schedule.getDmId())
               .eq(ScriptSchedule::getScheduleDate, schedule.getScheduleDate())
               .eq(ScriptSchedule::getIsDeleted, 0)
               .ne(ScriptSchedule::getStatus, 2)
               .ne(ScriptSchedule::getId, schedule.getId())
               .and(w -> w
                   .lt(ScriptSchedule::getStartTime, schedule.getEndTime())
                   .gt(ScriptSchedule::getEndTime, schedule.getStartTime())
               );
        Long count = scriptScheduleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BaseException("所选DM在该时段已有排期，请选择其他DM或调整时间");
        }
    }

    private void validateDmAssignment(ScriptSchedule schedule) {
        if (schedule.getDmId() == null || dmMapper == null) {
            return;
        }
        Dm dm = dmMapper.selectById(schedule.getDmId());
        if (dm == null || Integer.valueOf(1).equals(dm.getIsDeleted()) || !Integer.valueOf(1).equals(dm.getStatus())) {
            throw new BaseException("所选DM不存在或不可用");
        }
        if (schedule.getStoreId() != null && dm.getStoreId() != null && !Objects.equals(schedule.getStoreId(), dm.getStoreId())) {
            throw new BaseException("所选DM不属于当前门店");
        }
    }
}
