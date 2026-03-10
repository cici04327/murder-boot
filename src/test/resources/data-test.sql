-- 测试数据初始化

-- 插入测试用户 (密码: 123456 的 BCrypt 加密)
INSERT INTO user (id, username, password, nickname, phone, status, role) VALUES
(1, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', '13800138000', 1, 0),
(2, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', '13800138001', 1, 1);

-- 插入测试门店
INSERT INTO store (id, name, address, phone, description, longitude, latitude, rating, status) VALUES
(1, '测试门店A', '北京市朝阳区测试路100号', '010-12345678', '这是测试门店A', 116.4074, 39.9042, 4.5, 1),
(2, '测试门店B', '北京市海淀区测试路200号', '010-87654321', '这是测试门店B', 116.3074, 39.9842, 4.8, 1),
(3, '已关闭门店', '北京市西城区测试路300号', '010-11111111', '这是已关闭门店', 116.3574, 39.9142, 3.5, 0);

-- 插入剧本分类
INSERT INTO script_category (id, name, description, sort, status) VALUES
(1, '恐怖', '恐怖惊悚类剧本', 1, 1),
(2, '推理', '推理破案类剧本', 2, 1),
(3, '情感', '情感沉浸类剧本', 3, 1),
(4, '欢乐', '欢乐搞笑类剧本', 4, 1);

-- 插入测试剧本
INSERT INTO script (id, name, category_id, description, type, difficulty, player_count, duration, price, rating, status) VALUES
(1, '午夜惊魂', 1, '一个发生在深夜的恐怖故事', 1, 3, 6, 4.0, 198.00, 4.5, 1),
(2, '谜案追踪', 2, '一起离奇的谋杀案等待你来侦破', 1, 2, 5, 3.5, 168.00, 4.8, 1),
(3, '情书', 3, '一段跨越时空的爱情故事', 2, 1, 4, 3.0, 128.00, 4.2, 1),
(4, '已下架剧本', 1, '已下架的剧本', 1, 1, 6, 4.0, 100.00, 3.0, 0);

-- 插入门店房间
INSERT INTO store_room (id, store_id, name, capacity, price, status) VALUES
(1, 1, 'A01包间', 8, 100.00, 1),
(2, 1, 'A02包间', 10, 150.00, 1),
(3, 2, 'B01包间', 6, 80.00, 1),
(4, 2, 'B02包间', 12, 200.00, 1);

-- 插入测试预约
INSERT INTO reservation (id, reservation_no, user_id, store_id, room_id, script_id, reservation_time, duration, player_count, total_price, status, pay_status, check_in_code, check_in_status) VALUES
(1, 'RSV202601230001', 1, 1, 1, 1, '2026-01-25 14:00:00', 4.0, 6, 198.00, 1, 1, '123456', 0),
(2, 'RSV202601230002', 1, 2, 3, 2, '2026-01-26 19:00:00', 3.5, 5, 168.00, 0, 0, '654321', 0);

-- 插入优惠券
INSERT INTO coupon (id, name, type, value, min_amount, start_time, end_time, total_count, status) VALUES
(1, '新用户优惠券', 1, 20.00, 100.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1000, 1),
(2, '满减优惠券', 1, 50.00, 200.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 500, 1);
