-- 修复 script_review 表缺少 dm_id 和 dm_rating 列
ALTER TABLE script_review 
  ADD COLUMN dm_id BIGINT DEFAULT NULL COMMENT 'DM主持人ID' AFTER difficulty_rating,
  ADD COLUMN dm_rating INT DEFAULT NULL COMMENT 'DM评分(1-5)' AFTER dm_id;
