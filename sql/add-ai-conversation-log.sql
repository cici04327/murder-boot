-- AI对话日志表
CREATE TABLE IF NOT EXISTS `ai_conversation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        BIGINT       DEFAULT NULL COMMENT '用户ID（匿名则为null）',
  `session_id`     VARCHAR(64)  DEFAULT NULL COMMENT '会话ID（前端生成）',
  `question`       TEXT         NOT NULL COMMENT '用户提问内容',
  `answer`         TEXT         DEFAULT NULL COMMENT 'AI回复内容',
  `page`           VARCHAR(128) DEFAULT NULL COMMENT '用户当前页面',
  `is_transferred` TINYINT      DEFAULT 0 COMMENT '是否触发转人工：0否，1是',
  `provider`       VARCHAR(32)  DEFAULT NULL COMMENT 'AI提供商',
  `model`          VARCHAR(64)  DEFAULT NULL COMMENT '模型名称',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话日志表';
