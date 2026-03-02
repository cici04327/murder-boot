-- 门店登录功能数据库迁移脚本
-- 为门店表添加登录账号和密码字段

-- 添加登录账号字段（唯一）
ALTER TABLE store ADD COLUMN login_account VARCHAR(50) UNIQUE COMMENT '门店登录账号';

-- 添加登录密码字段
ALTER TABLE store ADD COLUMN login_password VARCHAR(100) COMMENT '门店登录密码（MD5加密）';

-- 为现有门店生成默认登录账号（store_门店ID）
UPDATE store SET login_account = CONCAT('store_', id) WHERE login_account IS NULL;

-- 为现有门店设置默认密码（123456 的 MD5 值）
-- MD5('123456') = 'e10adc3949ba59abbe56e057f20f883e'
UPDATE store SET login_password = 'e10adc3949ba59abbe56e057f20f883e' WHERE login_password IS NULL;

-- 创建索引加速登录查询
CREATE INDEX idx_store_login_account ON store(login_account);

-- 示例：手动为某个门店设置登录账号和密码
-- UPDATE store SET login_account = 'my_store', login_password = MD5('my_password') WHERE id = 1;
