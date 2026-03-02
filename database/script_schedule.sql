-- 剧本排期表
-- 用于管理剧本在特定日期和时段的可预约情况

CREATE TABLE IF NOT EXISTS `script_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '排期ID',
    `store_id` BIGINT NOT NULL COMMENT '门店ID',
    `script_id` BIGINT NOT NULL COMMENT '剧本ID',
    `room_id` BIGINT COMMENT '房间ID',
    `schedule_date` DATE NOT NULL COMMENT '排期日期',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `max_players` INT DEFAULT 10 COMMENT '最大预约人数',
    `current_players` INT DEFAULT 0 COMMENT '当前已预约人数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1可预约，0已满，2已关闭',
    `remark` VARCHAR(500) COMMENT '备注',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：1删除，0未删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_store_date` (`store_id`, `schedule_date`),
    KEY `idx_script_date` (`script_id`, `schedule_date`),
    KEY `idx_room_date` (`room_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧本排期表';

-- 示例数据
-- INSERT INTO script_schedule (store_id, script_id, room_id, schedule_date, start_time, end_time, max_players)
-- VALUES (1, 1, 1, '2026-02-10', '10:00:00', '14:00:00', 6);
