-- 测试数据初始化

-- 插入测试用户 (密码: 123456 的 BCrypt 加密)
-- 密码为 123456 的 MD5 加密值: e10adc3949ba59abbe56e057f20f883e
INSERT INTO user (id, username, password, nickname, phone, status, role, is_deleted) VALUES
(1, 'testuser', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', '13800138000', 1, 'USER', 0),
(2, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '13800138001', 1, 'SUPER_ADMIN', 0);

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

-- 插入测试预约（使用正确的表名 reservation_order）
INSERT INTO reservation_order (id, order_no, user_id, store_id, room_id, script_id, reservation_time, duration, player_count, total_price, actual_amount, status, pay_status, check_in_code, check_in_status, refund_status, is_deleted) VALUES
(1, 'RSV202601230001', 1, 1, 1, 1, '2026-01-25 14:00:00', 4.0, 6, 198.00, 198.00, 2, 1, '123456', 1, 0, 0),
(2, 'RSV202601230002', 1, 2, 3, 2, '2026-01-26 19:00:00', 3.5, 5, 168.00, 168.00, 0, 0, '654321', 0, 0, 0),
(3, 'RSV202601230003', 1, 1, 2, 1, '2026-03-20 14:00:00', 4.0, 6, 198.00, 198.00, 2, 1, '789012', 0, 0, 0);

-- 插入优惠券
INSERT INTO coupon (id, name, type, discount_value, min_amount, valid_start, valid_end, total_count, received_count, status) VALUES
(1, '新用户优惠券', 1, 20.00, 100.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1000, 900, 1),
(2, '满减优惠券', 1, 50.00, 200.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 500, 450, 1),
(3, '8折折扣券', 2, 0.20, 100.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 200, 180, 1);

-- 插入VIP套餐数据
INSERT INTO vip_package (id, name, level, duration_days, duration_type, original_price, current_price, point_multiplier, coupon_count, priority_booking, exclusive_service, birthday_gift, exclusive_badge, sort_order, status) VALUES
(1, '见习侦探月卡', 1, 30, 1, 39.00, 29.00, 1.20, 2, 0, 0, 0, 0, 1, 1),
(2, '银章侦探月卡', 2, 30, 1, 79.00, 59.00, 1.50, 5, 1, 0, 0, 0, 2, 1),
(3, '金章侦探月卡', 3, 30, 1, 149.00, 99.00, 2.00, 10, 1, 1, 1, 1, 3, 1),
(4, '传奇侦探月卡', 4, 30, 1, 299.00, 199.00, 3.00, 15, 1, 1, 1, 1, 4, 1);

-- 插入测试留言数据
INSERT INTO feedback (id, user_id, name, contact, subject, message, status, is_deleted) VALUES
(1, 1, '测试用户', '13800138000', 'platform', '这是一条测试留言，用于集成测试', 0, 0),
(2, NULL, '游客用户', 'guest@test.com', 'feedback', '游客留言测试内容，用于集成测试', 0, 0);

-- 插入AI知识库测试数据
INSERT INTO ai_knowledge_base (id, category, title, content, keywords, priority, status, is_faq, faq_question, hit_count, is_deleted) VALUES
(1, 'reservation', '预约剧本杀流程', '预约流程测试内容', '预约,下单', 10, 1, 1, '如何预约剧本杀？', 5, 0),
(2, 'refund', '退款规则', '退款规则测试内容', '退款,取消', 9, 1, 1, '如何申请退款？', 3, 0),
(3, 'payment', '支付方式说明', '仅支持支付宝支付', '支付,支付宝', 8, 1, 1, '支持哪些支付方式？', 2, 0),
(4, 'vip', 'VIP会员等级与权益', 'VIP权益测试内容', 'VIP,会员', 7, 0, 0, NULL, 0, 0);

-- 插入AI知识命中日志测试数据
INSERT INTO ai_knowledge_hit_log (id, user_id, session_id, query, knowledge_id, knowledge_title, category, page) VALUES
(1, 1, 'session_test_1', '如何预约剧本杀？', 1, '预约剧本杀流程', 'reservation', '/script'),
(2, 1, 'session_test_1', '如何申请退款？', 2, '退款规则', 'refund', '/reservation/detail/1'),
(3, 1, 'session_test_2', '支持哪些支付方式？', 3, '支付方式说明', 'payment', '/payment/1');

-- 插入AI对话日志测试数据
INSERT INTO ai_conversation_log (id, user_id, session_id, question, answer, page, is_transferred, provider, model) VALUES
(1, 1, 'session_test_1', '如何预约剧本杀？', '请先选择剧本和门店后下单。', '/script', 0, 'mock', 'test-model');
