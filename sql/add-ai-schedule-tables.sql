-- AI智能DM分配相关表
-- 创建时间: 2026-03-30

-- AI排班任务记录表
CREATE TABLE IF NOT EXISTS `ai_schedule_task` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `store_id`       BIGINT       NOT NULL COMMENT '门店ID',
    `task_type`      VARCHAR(30)  NOT NULL COMMENT '任务类型：DM_ASSIGN-DM分配，EMPLOYEE_SCHEDULE-员工排班',
    `start_date`     DATE         NOT NULL COMMENT '排班开始日期',
    `end_date`       DATE         NOT NULL COMMENT '排班结束日期',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，RUNNING-处理中，DONE-完成，FAILED-失败',
    `total_count`    INT          NOT NULL DEFAULT 0 COMMENT '需处理总数',
    `success_count`  INT          NOT NULL DEFAULT 0 COMMENT '成功处理数',
    `result_summary` TEXT                  COMMENT 'AI生成的排班说明/总结',
    `error_msg`      VARCHAR(500)          COMMENT '失败原因',
    `trigger_type`   VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL-手动，AUTO-自动',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`    DATETIME              COMMENT '创建时间',
    `update_time`    DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_store_type` (`store_id`, `task_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI排班任务记录表';
