package com.murder.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.murder.entity.ServiceSession;
import com.murder.mapper.ServiceSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 客服会话定时任务
 */
@Component
@Slf4j
public class ServiceSessionTask {

    @Autowired
    private ServiceSessionMapper serviceSessionMapper;

    /**
     * 每5分钟扫描一次，将超过30分钟未被接入的等待会话自动标记为超时关闭（status=3）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void closeTimeoutWaitingSessions() {
        log.info("开始执行客服会话超时检查任务");
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
            LambdaUpdateWrapper<ServiceSession> wrapper = new LambdaUpdateWrapper<ServiceSession>()
                    .eq(ServiceSession::getStatus, 0)  // 等待接入
                    .lt(ServiceSession::getCreateTime, timeoutThreshold)
                    .set(ServiceSession::getStatus, 3)  // 超时关闭
                    .set(ServiceSession::getEndTime, LocalDateTime.now());
            int count = serviceSessionMapper.update(null, wrapper);
            if (count > 0) {
                log.info("已自动关闭 {} 个超时等待会话", count);
            }
        } catch (Exception e) {
            log.error("客服会话超时检查任务执行失败", e);
        }
    }

    /**
     * 每小时扫描一次，将超过2小时无消息的进行中会话自动关闭（status=3）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void closeInactiveSessions() {
        log.info("开始执行客服进行中会话不活跃检查任务");
        try {
            LocalDateTime inactiveThreshold = LocalDateTime.now().minusHours(2);
            LambdaUpdateWrapper<ServiceSession> wrapper = new LambdaUpdateWrapper<ServiceSession>()
                    .eq(ServiceSession::getStatus, 1)  // 进行中
                    .lt(ServiceSession::getUpdateTime, inactiveThreshold)
                    .set(ServiceSession::getStatus, 3)  // 超时关闭
                    .set(ServiceSession::getEndTime, LocalDateTime.now());
            int count = serviceSessionMapper.update(null, wrapper);
            if (count > 0) {
                log.info("已自动关闭 {} 个不活跃进行中会话", count);
            }
        } catch (Exception e) {
            log.error("客服不活跃会话检查任务执行失败", e);
        }
    }
}
