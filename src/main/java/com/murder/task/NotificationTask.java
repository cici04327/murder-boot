package com.murder.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Reservation;
import com.murder.entity.UserCoupon;
import com.murder.entity.UserVip;
import com.murder.mapper.ReservationMapper;
import com.murder.mapper.UserCouponMapper;
import com.murder.mapper.UserVipMapper;
import com.murder.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 通知定时任务
 */
@Slf4j
@Component
public class NotificationTask {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private UserVipMapper userVipMapper;

    /**
     * 预约提醒任务 - 每小时执行一次
     * 提醒2小时后开始的预约
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行
    public void reservationReminder() {
        log.info("开始执行预约提醒任务");
        
        try {
            // 查询即将开始的预约（2小时内开始，状态为已支付）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoHoursLater = now.plusHours(2);
            
            LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Reservation::getStatus, 2) // 已支付
                   .ge(Reservation::getReservationTime, now)
                   .le(Reservation::getReservationTime, twoHoursLater);
            
            List<Reservation> reservations = reservationMapper.selectList(wrapper);
            
            int count = 0;
            // 发送提醒通知
            for (Reservation reservation : reservations) {
                try {
                    notificationService.sendToUsers(
                        "预约提醒",
                        String.format("温馨提醒：您的预约（订单号：%s）将在2小时内开始，预约时间：%s，请准时到场！", 
                                reservation.getOrderNo(), reservation.getReservationTime()),
                        2, // 预约提醒
                        "reservation",
                        reservation.getId(),
                        reservation.getUserId()
                    );
                    count++;
                } catch (Exception e) {
                    log.error("发送预约提醒通知失败", e);
                }
            }
            
            log.info("预约提醒任务执行完成，发送通知数: {}", count);
        } catch (Exception e) {
            log.error("预约提醒任务执行失败", e);
        }
    }

    /**
     * 优惠券到期提醒 - 每天执行一次
     * 提醒3天后到期的优惠券
     */
    @Scheduled(cron = "0 0 9 * * ?") // 每天上午9点执行
    public void couponExpireReminder() {
        log.info("开始执行优惠券到期提醒任务");
        
        try {
            // 查询3天后到期的优惠券（状态为未使用）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysLater = now.plusDays(3);
            
            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getStatus, 1) // 未使用
                   .le(UserCoupon::getExpireTime, threeDaysLater)
                   .ge(UserCoupon::getExpireTime, now);
            
            List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
            
            int count = 0;
            for (UserCoupon userCoupon : userCoupons) {
                try {
                    // 计算剩余天数
                    long daysLeft = java.time.Duration.between(now, userCoupon.getExpireTime()).toDays();
                    
                    String content;
                    if (daysLeft <= 0) {
                        content = "您的优惠券即将到期（今天到期），请尽快使用！";
                    } else if (daysLeft == 1) {
                        content = "您的优惠券还有1天就要到期了，请尽快使用！";
                    } else {
                        content = String.format("您的优惠券还有%d天就要到期了，请尽快使用！", daysLeft);
                    }
                    
                    notificationService.sendToUsers(
                        "优惠券即将到期",
                        content,
                        3, // 优惠券到期
                        "coupon",
                        userCoupon.getCouponId(),
                        userCoupon.getUserId()
                    );
                    count++;
                } catch (Exception e) {
                    log.error("发送优惠券到期提醒失败", e);
                }
            }
            
            log.info("优惠券到期提醒任务执行完成，发送通知数: {}", count);
        } catch (Exception e) {
            log.error("优惠券到期提醒任务执行失败", e);
        }
    }

    /**
     * VIP到期提醒 - 每天上午10点执行
     * 提前3天提醒即将到期的VIP用户续费
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void vipExpireReminder() {
        log.info("开始执行VIP到期提醒任务");
        try {
            LocalDateTime now = LocalDateTime.now();
            // 查找3天内到期的有效VIP
            LocalDateTime threeDaysLater = now.plusDays(3);

            LambdaQueryWrapper<UserVip> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserVip::getStatus, 1)
                   .gt(UserVip::getEndTime, now)
                   .le(UserVip::getEndTime, threeDaysLater);

            List<UserVip> expiringVips = userVipMapper.selectList(wrapper);

            int count = 0;
            for (UserVip vip : expiringVips) {
                try {
                    long daysLeft = ChronoUnit.DAYS.between(now, vip.getEndTime());
                    String levelName = getLevelName(vip.getLevel());

                    String title = "⏰ VIP会员即将到期";
                    String content;
                    if (daysLeft <= 0) {
                        content = String.format("您的%s会员今日到期，续费后可继续享受专属权益，月度体验券等福利不间断～", levelName);
                    } else {
                        content = String.format(
                                "您的%s会员还有 %d 天到期（%d月%d日），续费后月度体验券等权益持续发放，不要错过～",
                                levelName, daysLeft,
                                vip.getEndTime().getMonthValue(), vip.getEndTime().getDayOfMonth());
                    }

                    notificationService.sendToUsers(title, content, 2, "VIP_EXPIRE_REMIND", vip.getId(), vip.getUserId());
                    count++;
                } catch (Exception e) {
                    log.error("发送VIP到期提醒失败，userId={}", vip.getUserId(), e);
                }
            }
            log.info("VIP到期提醒任务执行完成，发送通知数: {}", count);
        } catch (Exception e) {
            log.error("VIP到期提醒任务执行失败", e);
        }
    }

    private String getLevelName(Integer level) {
        switch (level) {
            case 1: return "见习侦探";
            case 2: return "银章侦探";
            case 3: return "金章侦探";
            case 4: return "传奇侦探";
            default: return "VIP";
        }
    }
}
