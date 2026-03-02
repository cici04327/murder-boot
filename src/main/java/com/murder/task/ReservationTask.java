package com.murder.task;

import com.murder.entity.Reservation;
import com.murder.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约定时任务
 */
@Component
@Slf4j
public class ReservationTask {

    @Autowired
    private ReservationService reservationService;

    /**
     * 自动完成预约订单
     * 每小时执行一次，将预约时间已过的订单标记为已完成
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时的整点执行
    public void autoCompleteReservations() {
        log.info("开始执行自动完成预约订单任务");
        
        try {
            // 获取所有应该完成的预约（预约时间+时长已过的已确认订单）
            List<Reservation> reservations = reservationService.getCompletableReservations();
            
            if (reservations.isEmpty()) {
                log.info("没有需要自动完成的预约订单");
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Reservation reservation : reservations) {
                try {
                    // 标记为已完成
                    reservationService.complete(reservation.getId());
                    successCount++;
                    log.info("自动完成预约订单: id={}, orderNo={}", 
                            reservation.getId(), reservation.getOrderNo());
                } catch (Exception e) {
                    failCount++;
                    log.error("自动完成预约订单失败: id={}, error={}", 
                            reservation.getId(), e.getMessage(), e);
                }
            }
            
            log.info("自动完成预约订单任务执行完毕: 成功={}, 失败={}, 总数={}", 
                    successCount, failCount, reservations.size());
        } catch (Exception e) {
            log.error("执行自动完成预约订单任务失败", e);
        }
    }
    
    /**
     * 自动取消超时未支付的预约
     * 每30分钟执行一次，取消创建超过2小时仍未支付的订单
     */
    @Scheduled(cron = "0 */30 * * * ?") // 每30分钟执行一次
    public void autoCancelUnpaidReservations() {
        log.info("开始执行自动取消超时未支付预约任务");
        
        try {
            // 获取超时未支付的预约（创建2小时后仍未支付的待支付订单）
            LocalDateTime timeoutTime = LocalDateTime.now().minusHours(2);
            List<Reservation> reservations = reservationService.getUnpaidReservations(timeoutTime);
            
            if (reservations.isEmpty()) {
                log.info("没有需要自动取消的预约订单");
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Reservation reservation : reservations) {
                try {
                    // 取消预约
                    reservationService.cancel(reservation.getId(), "系统自动取消：超时未支付");
                    successCount++;
                    log.info("自动取消超时未支付预约: id={}, orderNo={}", 
                            reservation.getId(), reservation.getOrderNo());
                } catch (Exception e) {
                    failCount++;
                    log.error("自动取消预约失败: id={}, error={}", 
                            reservation.getId(), e.getMessage(), e);
                }
            }
            
            log.info("自动取消超时未支付预约任务执行完毕: 成功={}, 失败={}, 总数={}", 
                    successCount, failCount, reservations.size());
        } catch (Exception e) {
            log.error("执行自动取消超时未支付预约任务失败", e);
        }
    }
}
