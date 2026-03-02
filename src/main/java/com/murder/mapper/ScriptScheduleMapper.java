package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.ScriptSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 剧本排期Mapper
 */
@Mapper
public interface ScriptScheduleMapper extends BaseMapper<ScriptSchedule> {

    /**
     * 查询门店某日期的排期列表（带剧本名和房间名）
     */
    @Select("SELECT ss.*, s.name as script_name, sr.name as room_name " +
            "FROM script_schedule ss " +
            "LEFT JOIN script s ON ss.script_id = s.id " +
            "LEFT JOIN store_room sr ON ss.room_id = sr.id " +
            "WHERE ss.store_id = #{storeId} " +
            "AND ss.schedule_date = #{scheduleDate} " +
            "AND ss.is_deleted = 0 " +
            "ORDER BY ss.start_time")
    List<ScriptSchedule> listByStoreAndDate(@Param("storeId") Long storeId, 
                                            @Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询门店日期范围内的排期
     */
    @Select("SELECT ss.*, s.name as script_name, sr.name as room_name " +
            "FROM script_schedule ss " +
            "LEFT JOIN script s ON ss.script_id = s.id " +
            "LEFT JOIN store_room sr ON ss.room_id = sr.id " +
            "WHERE ss.store_id = #{storeId} " +
            "AND ss.schedule_date BETWEEN #{startDate} AND #{endDate} " +
            "AND ss.is_deleted = 0 " +
            "ORDER BY ss.schedule_date, ss.start_time")
    List<ScriptSchedule> listByStoreAndDateRange(@Param("storeId") Long storeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
