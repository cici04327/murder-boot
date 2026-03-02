-- ============================================
-- 剧本推荐系统数据库表
-- ============================================

-- ----------------------------
-- 用户浏览记录表
-- ----------------------------
DROP TABLE IF EXISTS `user_browse_history`;
CREATE TABLE `user_browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1剧本，2门店',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `browse_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `duration` int DEFAULT 0 COMMENT '浏览时长（秒）',
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_target` (`target_type`, `target_id`) USING BTREE,
  KEY `idx_browse_time` (`browse_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户浏览记录表';

-- ----------------------------
-- 推荐记录表
-- ----------------------------
DROP TABLE IF EXISTS `recommendation_log`;
CREATE TABLE `recommendation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `script_id` bigint NOT NULL COMMENT '剧本ID',
  `recommendation_type` tinyint NOT NULL COMMENT '推荐类型：1协同过滤，2内容推荐，3热门推荐，4基于历史',
  `score` decimal(5,2) DEFAULT 0.00 COMMENT '推荐分数',
  `is_clicked` tinyint DEFAULT 0 COMMENT '是否点击：1是，0否',
  `click_time` datetime DEFAULT NULL COMMENT '点击时间',
  `is_reserved` tinyint DEFAULT 0 COMMENT '是否预约：1是，0否',
  `reserve_time` datetime DEFAULT NULL COMMENT '预约时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_script` (`user_id`, `script_id`) USING BTREE,
  KEY `idx_recommendation_type` (`recommendation_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐记录表';

-- ----------------------------
-- 剧本标签表（用于内容推荐）
-- ----------------------------
DROP TABLE IF EXISTS `script_tag`;
CREATE TABLE `script_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `script_id` bigint NOT NULL COMMENT '剧本ID',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_type` tinyint DEFAULT 1 COMMENT '标签类型：1风格，2难度，3主题，4特色',
  `weight` decimal(3,2) DEFAULT 1.00 COMMENT '权重',
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_script_id` (`script_id`) USING BTREE,
  KEY `idx_tag_name` (`tag_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='剧本标签表';

-- ----------------------------
-- 用户偏好表
-- ----------------------------
DROP TABLE IF EXISTS `user_preference`;
CREATE TABLE `user_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `preference_type` varchar(50) NOT NULL COMMENT '偏好类型：category_恐怖, difficulty_2, tag_推理',
  `preference_value` varchar(50) NOT NULL COMMENT '偏好值',
  `score` decimal(5,2) DEFAULT 0.00 COMMENT '偏好分数（越高越喜欢）',
  `count` int DEFAULT 1 COMMENT '偏好次数',
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_preference` (`user_id`, `preference_type`, `preference_value`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户偏好表';

-- ----------------------------
-- 热门榜单缓存表
-- ----------------------------
DROP TABLE IF EXISTS `hot_ranking`;
CREATE TABLE `hot_ranking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ranking_type` tinyint NOT NULL COMMENT '榜单类型：1今日热门，2本周热门，3本月热门，4口碑榜',
  `script_id` bigint NOT NULL COMMENT '剧本ID',
  `rank` int NOT NULL COMMENT '排名',
  `score` decimal(10,2) DEFAULT 0.00 COMMENT '热度分数',
  `view_count` int DEFAULT 0 COMMENT '浏览量',
  `reserve_count` int DEFAULT 0 COMMENT '预约量',
  `favorite_count` int DEFAULT 0 COMMENT '收藏量',
  `rating` decimal(3,2) DEFAULT 0.00 COMMENT '评分',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ranking_script` (`ranking_type`, `script_id`) USING BTREE,
  KEY `idx_ranking_type` (`ranking_type`, `rank`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='热门榜单缓存表';

-- ----------------------------
-- 初始化一些示例数据
-- ----------------------------

-- 为现有剧本添加一些标签示例
INSERT INTO `script_tag` (`script_id`, `tag_name`, `tag_type`, `weight`) VALUES
(1, '恐怖', 1, 1.0),
(1, '惊悚', 1, 0.9),
(1, '推理', 3, 0.7),
(2, '推理', 1, 1.0),
(2, '烧脑', 1, 0.9),
(2, '硬核', 2, 0.8),
(3, '情感', 1, 1.0),
(3, '治愈', 1, 0.9),
(3, '轻松', 2, 0.7);

SET FOREIGN_KEY_CHECKS = 1;
