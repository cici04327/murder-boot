package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.ScriptSchedule;
import com.murder.mapper.ScriptScheduleMapper;
import com.murder.service.ScriptScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 剧本排期服务实现类
 */
@Slf4j
@Service
public class ScriptScheduleServiceImpl implements ScriptScheduleService {

    @Autowired
    private ScriptScheduleMapper scriptScheduleMapper;

    @Override
    public List<ScriptSchedule> listByStoreAndDate(Long storeId, LocalDate scheduleDate) {
        return scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate);
    }

    @Override
    public List<ScriptSchedule> listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        return scriptScheduleMapper.listByStoreAndDateRange(storeId, startDate, endDate);
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
        scriptScheduleMapper.updateById(schedule);
        log.info("更新剧本排期: id={}", schedule.getId());
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
