package com.murder.task;

import com.murder.service.GroupOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拼单定时任务
 */
@Component
@Slf4j
public class GroupOrderTask {

    private final GroupOrderService groupOrderService;

    public GroupOrderTask(GroupOrderService groupOrderService) {
        this.groupOrderService = groupOrderService;
    }

    /**
     * 处理24小时内未成团的拼单
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void processExpiredGroups() {
        log.info("开始处理超时未成团拼单");
        try {
            groupOrderService.processExpiredGroups();
        } catch (Exception e) {
            log.error("处理超时未成团拼单失败", e);
        }
    }
}
