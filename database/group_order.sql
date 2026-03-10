-- 拼单表
CREATE TABLE IF NOT EXISTS `group_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '拼单ID',
  `creator_id` bigint NOT NULL COMMENT '发起人ID',
  `creator_name` varchar(50) DEFAULT NULL COMMENT '发起人名称',
  `creator_avatar` varchar(255) DEFAULT NULL COMMENT '发起人头像',
  `script_id` bigint NOT NULL COMMENT '剧本ID',
  `script_name` varchar(100) NOT NULL COMMENT '剧本名称',
  `store_id` bigint NOT NULL COMMENT '门店ID',
  `store_name` varchar(100) NOT NULL COMMENT '门店名称',
  `play_time` datetime NOT NULL COMMENT '开车时间',
  `current_count` int NOT NULL DEFAULT '1' COMMENT '当前人数',
  `need_count` int NOT NULL COMMENT '需要总人数',
  `player_count` int NOT NULL COMMENT '剧本人数',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格（每人）',
  `gender_requirement` varchar(50) DEFAULT '男女不限' COMMENT '性别要求',
  `newbie_welcome` tinyint DEFAULT '1' COMMENT '新手友好 0-否 1-是',
  `description` varchar(500) DEFAULT NULL COMMENT '拼单说明',
  `reservation_id` bigint DEFAULT NULL COMMENT '关联预约ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-已取消 1-拼团中 2-已成团 3-已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_script_id` (`script_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`),
  KEY `idx_play_time` (`play_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拼单表';

-- 拼单成员表
CREATE TABLE IF NOT EXISTS `group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `group_id` bigint NOT NULL COMMENT '拼单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `is_creator` tinyint DEFAULT '0' COMMENT '是否发起人 0-否 1-是',
  `join_count` int DEFAULT '1' COMMENT '参与人数（该用户带的人数）',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-已退出 1-已加入',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拼单成员表';

-- 插入测试数据
INSERT INTO `group_order` (`creator_id`, `creator_name`, `creator_avatar`, `script_id`, `script_name`, `store_id`, `store_name`, `play_time`, `current_count`, `need_count`, `player_count`, `price`, `gender_requirement`, `newbie_welcome`, `description`, `status`) VALUES
(1, '小明', NULL, 1, '年轮', 1, '迷雾剧本杀', DATE_ADD(NOW(), INTERVAL 2 DAY), 2, 6, 6, 128.00, '男女不限', 1, '新手局，欢迎萌新一起来玩！', 1);

-- 插入测试成员数据
INSERT INTO `group_member` (`group_id`, `user_id`, `nickname`, `avatar`, `is_creator`, `join_count`, `status`) VALUES
(1, 1, '小明', NULL, 1, 1, 1),
(1, 5, '玩家A', NULL, 0, 1, 1);
