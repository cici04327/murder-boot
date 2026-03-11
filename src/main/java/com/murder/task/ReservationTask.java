package com.murder.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.GroupOrder;
import com.murder.entity.Reservation;
import com.murder.entity.User;
import com.murder.entity.UserVip;
import com.murder.mapper.UserMapper;
import com.murder.mapper.UserVipMapper;
import com.murder.service.GroupOrderService;
import com.murder.service.ReservationService;
import com.murder.service.VipService;
import com.murder.service.impl.VipServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    @Autowired
    private VipService vipService;

    @Autowired
    private VipServiceImpl vipServiceImpl;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserVipMapper userVipMapper;

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
     * 自动取消超时未支付的预约（每5分钟扫描一次，超过30分钟未支付自动关闭）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCancelUnpaidReservations() {
        log.info("开始执行自动取消超时未支付预约任务");

        try {
            LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);
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

    /**
     * 每月1日凌晨1点：为所有有效VIP用户发放月度体验券
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void grantMonthlyVipCoupons() {
        log.info("开始执行VIP月度体验券发放任务");
        try {
            LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserVip::getStatus, 1)
                   .gt(UserVip::getEndTime, LocalDateTime.now());
            List<UserVip> activeVips = userVipMapper.selectList(wrapper);
            int success = 0;
            for (UserVip vip : activeVips) {
                try {
                    vipService.grantMonthlyCoupons(vip.getUserId());
                    success++;
                } catch (Exception e) {
                    log.error("为用户 {} 发放月度体验券失败", vip.getUserId(), e);
                }
            }
            log.info("VIP月度体验券发放完成，成功：{}/{}，时间：{}", success, activeVips.size(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("VIP月度体验券发放任务异常", e);
        }
    }

    /**
     * 每天早上9点：为当日生日的VIP用户发放生日券
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void grantBirthdayCoupons() {
        log.info("开始执行VIP生日券发放任务");
        try {
            // 查询当日生日的VIP用户（birthday字段格式 MM-dd 或 LocalDate）
            LocalDate today = LocalDate.now();
            int month = today.getMonthValue();
            int day = today.getDayOfMonth();

            // 查找生日为今天的用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.isNotNull(User::getBirthday);
            List<User> users = userMapper.selectList(userWrapper);

            int success = 0;
            for (User user : users) {
                try {
                    if (user.getBirthday() == null) continue;
                    // 判断生日月日是否匹配今天
                    LocalDate birthday = user.getBirthday();
                    if (birthday.getMonthValue() == month && birthday.getDayOfMonth() == day) {
                        // 判断是否是VIP
                        if (vipService.isVip(user.getId())) {
                            vipServiceImpl.grantBirthdayCoupon(user.getId());
                            success++;
                            log.info("为生日VIP用户 {} 发放生日专享券", user.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("为用户 {} 发放生日券失败", user.getId(), e);
                }
            }
            log.info("VIP生日券发放完成，今日生日VIP用户共{}人", success);
        } catch (Exception e) {
            log.error("VIP生日券发放任务异常", e);
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
