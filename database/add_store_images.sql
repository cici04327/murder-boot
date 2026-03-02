-- 为 store 表添加 images 字段（如果不存在）
-- 执行前请先检查数据库是否已有该字段

ALTER TABLE `store` 
ADD COLUMN `images` VARCHAR(1000) NULL COMMENT '门店图片（多张图片用逗号分隔）' AFTER `phone`;
