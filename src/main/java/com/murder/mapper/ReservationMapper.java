package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约映射�?
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
    
    /**
     * 查询即将开始的预约（指定小时数内）
     * @param startTime 开始时�?
     * @param endTime 结束时间
     * @return 预约列表
     */
    @Select("SELECT * FROM reservation_order WHERE status IN (1, 2) AND reservation_time >= #{startTime} AND reservation_time <= #{endTime} AND is_deleted = 0")
    List<Reservation> selectUpcomingReservations(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 检查房间在指定时间段内是否有冲突的预约
     * @param roomId 房间ID
     * @param startTime 开始时�?
     * @param endTime 结束时间
     * @return 冲突的预约数�?
     */
    @Select("SELECT COUNT(*) FROM reservation_order WHERE room_id = #{roomId} " +
            "AND status IN (1, 2) " +
            "AND is_deleted = 0 " +
            "AND reservation_time < #{endTime} " +
            "AND TIMESTAMPADD(MINUTE, CAST(CEILING(COALESCE(duration, 3.0) * 60) AS SIGNED), reservation_time) > #{startTime}")
    int countConflictingReservations(@Param("roomId") Long roomId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
}
