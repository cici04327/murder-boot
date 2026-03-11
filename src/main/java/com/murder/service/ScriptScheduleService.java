package com.murder.service;

import com.murder.entity.ScriptSchedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 剧本排期服务接口
 */
public interface ScriptScheduleService {

    /**
     * 查询门店某日期的排期列表
     */
    List<ScriptSchedule> listByStoreAndDate(Long storeId, LocalDate scheduleDate);

    /**
     * 查询门店日期范围内的排期
     */
    List<ScriptSchedule> listByStoreAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate);

    /**
     * 新增排期
     */
    void add(ScriptSchedule schedule);

    /**
     * 批量新增排期
     */
    void batchAdd(List<ScriptSchedule> schedules);

    /**
     * 更新排期
     */
    void update(ScriptSchedule schedule);

    /**
     * 删除排期
     */
    void delete(Long id);

    /**
     * 根据ID查询排期
     */
    ScriptSchedule getById(Long id);

    /**
     * 更新排期状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 批量生成排期（根据模板）
     */
    void generateSchedules(Long storeId, Long scriptId, Long roomId, 
                           LocalDate startDate, LocalDate endDate,
                           List<String> timeSlots, Integer maxPlayers);

    /**
     * 增加排期已预约人数（预约创建时调用）
     */
    void incrementCurrentPlayers(Long scheduleId, int count);

    /**
     * 减少排期已预约人数（预约取消时调用）
     */
    void decrementCurrentPlayers(Long scheduleId, int count);

    /**
     * 查询指定剧本在指定门店近N天的可约场次（含余量），供用户端展示
     * @param scriptId 剧本ID（可选）
     * @param storeId  门店ID（可选）
     * @param days     查询未来天数（默认7天）
     * @return 可约场次列表（status=1 且 currentPlayers < maxPlayers）
     */
    List<ScriptSchedule> getAvailableSchedules(Long scriptId, Long storeId, Integer days);
}
