package com.murder.task;

import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.service.GroupOrderService;
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

    @Autowired
    private GroupOrderService groupOrderService;

    /**
     * 自动完成已到结束时间的预约
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCompleteReservations() {
        log.info("开始执行自动完成预约订单任务");

        try {
            List<Reservation> reservations = reservationService.getCompletableReservations();
            if (reservations.isEmpty()) {
                log.info("没有需要自动完成的预约订单");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            for (Reservation reservation : reservations) {
                try {
                    reservationService.complete(reservation.getId());
                    successCount++;
                    log.info("自动完成预约订单: id={}, orderNo={}", reservation.getId(), reservation.getOrderNo());
                } catch (Exception e) {
                    failCount++;
                    log.error("自动完成预约订单失败: id={}", reservation.getId(), e);
                }
            }

            log.info("自动完成预约任务结束: success={}, fail={}, total={}", successCount, failCount, reservations.size());
        } catch (Exception e) {
            log.error("执行自动完成预约任务失败", e);
        }
    }

    /**
     * 自动取消超时未支付的预约
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void autoCancelUnpaidReservations() {
        log.info("开始执行自动取消超时未支付预约任务");

        try {
            LocalDateTime timeoutTime = LocalDateTime.now().minusHours(2);
            List<Reservation> reservations = reservationService.getUnpaidReservations(timeoutTime);

            if (reservations.isEmpty()) {
                log.info("没有需要自动取消的预约订单");
                return;
            }

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            for (Reservation reservation : reservations) {
                try {
                    if (shouldSkipAutoCancel(reservation, timeoutTime)) {
                        skipCount++;
                        continue;
                    }

                    reservationService.cancel(reservation.getId(), "系统自动取消：超时未支付");
                    successCount++;
                    log.info("自动取消超时未支付预约: id={}, orderNo={}", reservation.getId(), reservation.getOrderNo());
                } catch (Exception e) {
                    failCount++;
                    log.error("自动取消预约失败: id={}", reservation.getId(), e);
                }
            }

            log.info("自动取消未支付预约任务结束: success={}, skip={}, fail={}, total={}",
                    successCount, skipCount, failCount, reservations.size());
        } catch (Exception e) {
            log.error("执行自动取消未支付预约任务失败", e);
        }
    }

    private boolean shouldSkipAutoCancel(Reservation reservation, LocalDateTime timeoutTime) {
        if (reservation.getGroupId() == null) {
            return false;
        }

        GroupOrder groupOrder = groupOrderService.getById(reservation.getGroupId());
        if (groupOrder == null) {
            return false;
        }

        if (Integer.valueOf(1).equals(groupOrder.getStatus())) {
            return true;
        }

        if (Integer.valueOf(2).equals(groupOrder.getStatus())) {
            LocalDateTime successTime = groupOrder.getUpdateTime() != null ? groupOrder.getUpdateTime() : groupOrder.getCreateTime();
            return successTime != null && successTime.isAfter(timeoutTime);
        }

        return false;
    }
}
