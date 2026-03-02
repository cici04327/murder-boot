package com.murder.task;

import com.murder.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐系统定时任务
 */
@Slf4j
@Component
public class RecommendationTask {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 每小时刷新今日热门榜单
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshDailyHotRanking() {
        log.info("开始刷新今日热门榜单");
        try {
            recommendationService.refreshHotRanking(1);
            log.info("今日热门榜单刷新成功");
        } catch (Exception e) {
            log.error("刷新今日热门榜单失败", e);
        }
    }

    /**
     * 每天凌晨1点刷新本周热门榜单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshWeeklyHotRanking() {
        log.info("开始刷新本周热门榜单");
        try {
            recommendationService.refreshHotRanking(2);
            log.info("本周热门榜单刷新成功");
        } catch (Exception e) {
            log.error("刷新本周热门榜单失败", e);
        }
    }

    /**
     * 每天凌晨2点刷新本月热门榜单
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshMonthlyHotRanking() {
        log.info("开始刷新本月热门榜单");
        try {
            recommendationService.refreshHotRanking(3);
            log.info("本月热门榜单刷新成功");
        } catch (Exception e) {
            log.error("刷新本月热门榜单失败", e);
        }
    }

    /**
     * 每天凌晨3点刷新口碑榜
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void refreshReputationRanking() {
        log.info("开始刷新口碑榜");
        try {
            recommendationService.refreshHotRanking(4);
            log.info("口碑榜刷新成功");
        } catch (Exception e) {
            log.error("刷新口碑榜失败", e);
        }
    }
}
