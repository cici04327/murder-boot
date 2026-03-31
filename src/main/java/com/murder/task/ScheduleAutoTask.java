package com.murder.task;

import com.murder.service.AiScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI智能DM分配定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAutoTask {

    private final AiScheduleService aiScheduleService;

    /**
     * 每周一凌晨3点：自动为下周排期智能分配DM
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void autoAssignDmForNextWeek() {
        log.info("[AutoTask] Start auto assigning DM for next week schedules");
        try {
            aiScheduleService.autoAssignDmForWeekSchedules();
            log.info("[AutoTask] DM auto assignment completed");
        } catch (Exception e) {
            log.error("[AutoTask] Failed to auto assign DM", e);
        }
    }
}
