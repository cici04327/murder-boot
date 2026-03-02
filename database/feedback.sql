-- 用户留言/反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（可为空，游客也可留言）',
    `name` VARCHAR(50) NOT NULL COMMENT '留言人姓名',
    `contact` VARCHAR(100) NOT NULL COMMENT '联系方式（手机号或邮箱）',
    `subject` VARCHAR(50) NOT NULL COMMENT '主题类型：platform-平台使用问题, booking-预约相关, account-账号相关, feedback-建议与反馈, business-商务合作, other-其他',
    `message` TEXT NOT NULL COMMENT '留言内容',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待处理，1处理中，2已回复，3已关闭',
    `reply_content` TEXT DEFAULT NULL COMMENT '管理员回复内容',
    `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
    `reply_user_id` BIGINT DEFAULT NULL COMMENT '回复管理员ID',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1删除，0未删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_subject` (`subject`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户留言反馈表';
