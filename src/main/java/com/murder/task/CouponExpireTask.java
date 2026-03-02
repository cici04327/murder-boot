package com.murder.task;

import com.murder.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券过期定时任务
 */
@Component
@Slf4j
public class CouponExpireTask {

    @Autowired
    private CouponService couponService;

    /**
     * 每天凌晨1点执行优惠券过期检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void expireCoupons() {
        log.info("开始执行优惠券过期检查任务");
        try {
            couponService.expireCoupons();
            log.info("优惠券过期检查任务执行完成");
        } catch (Exception e) {
            log.error("优惠券过期检查任务执行失败", e);
        }
    }
}
