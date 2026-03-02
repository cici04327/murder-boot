package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.ReservationDTO;
import com.murder.entity.Reservation;

import java.util.List;

/**
 * 预约服务接口
 */
public interface ReservationService {

    /**
     * 创建预约
     */
    Reservation create(ReservationDTO reservationDTO);

    /**
     * 分页查询预约列表
     */
    PageResult<Reservation> pageQuery(Integer page, Integer pageSize, Long userId, Integer status);
    
    /**
     * 分页查询预约列表（包含关联信息）
     */
    PageResult<com.murder.vo.ReservationVO> pageQueryWithDetails(Integer page, Integer pageSize, Long userId, Long storeId, Integer status, Integer refundStatus, Boolean hasRefund);

    /**
     * 根据ID查询预约详情
     */
    Reservation getById(Long id);
    
    /**
     * 根据ID查询预约详情VO（包含关联信息）
     */
    com.murder.vo.ReservationVO getDetailById(Long id);

    /**
     * 根据预约编号查询预约详情
     */
    Reservation getByReservationNo(String reservationNo);

    /**
     * 确认预约
     */
    void confirm(Long id);

    /**
     * 取消预约
     */
    void cancel(Long id, String reason);

    /**
     * 完成预约
     */
    void complete(Long id);
    
    /**
     * 获取可以完成的预约列表（预约时间+时长已过的已确认订单�?
     */
    List<Reservation> getCompletableReservations();
    
    /**
     * 获取超时未支付的预约列表
     */
    List<Reservation> getUnpaidReservations(java.time.LocalDateTime timeoutTime);

    /**
     * 支付预约
     */
    void pay(Long id);
    
    /**
     * 查询即将开始的预约（指定小时数内）
     */
    List<Reservation> getUpcomingReservations(Integer hours);
    
    /**
     * 检查房间可用�?
     */
    boolean checkRoomAvailability(Long roomId, String reservationTime, Double duration);
}