-- 测试数据库初始化脚本

-- 用户表（与 User 实体类字段对齐）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    gender INT DEFAULT 0,
    member_level INT DEFAULT 0,
    vip_level INT DEFAULT 0,
    vip_expire_time DATETIME,
    points INT DEFAULT 0,
    status INT DEFAULT 1,
    role VARCHAR(50) DEFAULT 'USER',
    store_id BIGINT,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 门店表（字段与 Store 实体类对齐）
CREATE TABLE IF NOT EXISTS store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20),
    description TEXT,
    cover VARCHAR(255),
    images TEXT,
    longitude DECIMAL(10, 7),
    latitude DECIMAL(10, 7),
    open_time TIME,
    close_time TIME,
    rating DECIMAL(2, 1) DEFAULT 0,
    status INT DEFAULT 1,
    login_account VARCHAR(50),
    login_password VARCHAR(100),
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 剧本分类表（字段与实体类对齐）
CREATE TABLE IF NOT EXISTS script_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 剧本表（字段与 Script 实体类对齐）
CREATE TABLE IF NOT EXISTS script (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT,
    cover VARCHAR(255),
    images VARCHAR(1000),
    description TEXT,
    type INT DEFAULT 1,
    difficulty INT DEFAULT 1,
    player_count INT DEFAULT 6,
    duration DECIMAL(3, 1) DEFAULT 4.0,
    price DECIMAL(10, 2),
    tags VARCHAR(255),
    rating DECIMAL(2, 1) DEFAULT 0,
    is_exclusive INT DEFAULT 0,
    play_count INT DEFAULT 0,
    status INT DEFAULT 1,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 门店房间表
CREATE TABLE IF NOT EXISTS store_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type INT DEFAULT 1,
    capacity INT DEFAULT 10,
    price DECIMAL(10, 2),
    description VARCHAR(255),
    status INT DEFAULT 1,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 预约表（表名与实体类 @TableName("reservation_order") 对齐）
CREATE TABLE IF NOT EXISTS reservation_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    room_id BIGINT,
    script_id BIGINT,
    group_id BIGINT,
    schedule_id BIGINT,
    dm_id BIGINT,
    player_count INT DEFAULT 1,
    reservation_time DATETIME NOT NULL,
    duration DECIMAL(3, 1) DEFAULT 3.0,
    total_price DECIMAL(10, 2),
    coupon_id BIGINT,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    vip_discount_amount DECIMAL(10, 2) DEFAULT 0,
    vip_discount DECIMAL(4, 2),
    actual_amount DECIMAL(10, 2),
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    remark VARCHAR(255),
    status INT DEFAULT 0,
    pay_status INT DEFAULT 0,
    pay_time DATETIME,
    refund_reason VARCHAR(255),
    refund_apply_time DATETIME,
    refund_process_time DATETIME,
    refund_status INT DEFAULT 0,
    check_in_code VARCHAR(20),
    check_in_status INT DEFAULT 0,
    check_in_time DATETIME,
    admin_remark VARCHAR(255),
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- DM（主持人）表
CREATE TABLE IF NOT EXISTS dm (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    introduction TEXT,
    style_tags VARCHAR(255),
    rating DECIMAL(2,1) DEFAULT 0.0,
    game_count INT DEFAULT 0,
    status INT DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 剧本排期表
CREATE TABLE IF NOT EXISTS script_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    script_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    dm_id BIGINT,
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_players INT NOT NULL DEFAULT 6,
    current_players INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 优惠券表（字段与实体类对齐）
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type INT DEFAULT 1,
    discount_value DECIMAL(10, 2),
    min_amount DECIMAL(10, 2) DEFAULT 0,
    total_count INT DEFAULT 0,
    received_count INT DEFAULT 0,
    valid_start DATETIME,
    valid_end DATETIME,
    exchange_points INT DEFAULT 0,
    description VARCHAR(500),
    status INT DEFAULT 1,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户优惠券表（字段与 UserCoupon 实体类对齐）
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status INT DEFAULT 0,
    receive_time DATETIME,
    expire_time DATETIME,
    use_time DATETIME,
    order_id BIGINT,
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 通知系统表
CREATE TABLE IF NOT EXISTS system_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type INT DEFAULT 1,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    target_type INT DEFAULT 1,
    target_users VARCHAR(2000),
    send_time DATETIME,
    status INT DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户通知记录表
CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    is_read INT DEFAULT 0,
    read_time DATETIME,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 留言反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(50),
    contact VARCHAR(100),
    subject VARCHAR(50),
    message TEXT,
    status INT DEFAULT 0,
    reply_content TEXT,
    reply_time DATETIME,
    reply_user_id BIGINT,
    ip_address VARCHAR(50),
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- VIP套餐表（字段与 VipPackage 实体类对齐）
CREATE TABLE IF NOT EXISTS vip_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    level INT NOT NULL,
    duration_days INT NOT NULL,
    duration_type INT DEFAULT 1,
    original_price DECIMAL(10,2),
    current_price DECIMAL(10,2),
    discount_rate DECIMAL(4,2),
    special_discount DECIMAL(4,2),
    point_multiplier DECIMAL(4,2) DEFAULT 1.00,
    coupon_count INT DEFAULT 0,
    priority_booking INT DEFAULT 0,
    exclusive_service INT DEFAULT 0,
    birthday_gift INT DEFAULT 0,
    exclusive_badge INT DEFAULT 0,
    description VARCHAR(500),
    features TEXT,
    tag VARCHAR(100),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户VIP记录表
CREATE TABLE IF NOT EXISTS user_vip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id BIGINT,
    level INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    original_price DECIMAL(10,2),
    actual_price DECIMAL(10,2),
    payment_method VARCHAR(50),
    order_no VARCHAR(50),
    status INT DEFAULT 1,
    auto_renew INT DEFAULT 0,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 管理员通知表（字段与 AdminNotification 实体类对齐）
CREATE TABLE IF NOT EXISTS admin_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type INT DEFAULT 1,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    target_type INT DEFAULT 1,
    store_id BIGINT,
    priority INT DEFAULT 1,
    is_read INT DEFAULT 0,
    read_time DATETIME,
    send_time DATETIME,
    status INT DEFAULT 1,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 门店评价表（store_review）
CREATE TABLE IF NOT EXISTS store_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    script_id BIGINT,
    dm_id BIGINT DEFAULT NULL,
    overall_rating INT DEFAULT 5,
    script_rating INT DEFAULT 5,
    service_rating INT DEFAULT 5,
    dm_rating INT DEFAULT NULL,
    content TEXT,
    images VARCHAR(1000),
    tags VARCHAR(255),
    is_anonymous INT DEFAULT 0,
    status INT DEFAULT 1,
    admin_reply TEXT,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 评价表
CREATE TABLE IF NOT EXISTS review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    script_id BIGINT,
    dm_id BIGINT DEFAULT NULL,
    overall_rating INT DEFAULT 5,
    script_rating INT DEFAULT 5,
    service_rating INT DEFAULT 5,
    dm_rating INT DEFAULT NULL,
    content TEXT,
    images VARCHAR(1000),
    tags VARCHAR(255),
    is_anonymous INT DEFAULT 0,
    status INT DEFAULT 1,
    admin_reply TEXT,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI知识库表
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    keywords VARCHAR(500),
    priority INT NOT NULL DEFAULT 5,
    status INT NOT NULL DEFAULT 1,
    is_faq INT NOT NULL DEFAULT 0,
    faq_question VARCHAR(255),
    hit_count INT NOT NULL DEFAULT 0,
    last_hit_time DATETIME,
    is_deleted INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI知识命中日志表
CREATE TABLE IF NOT EXISTS ai_knowledge_hit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    query VARCHAR(500) NOT NULL,
    knowledge_id BIGINT NOT NULL,
    knowledge_title VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    page VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI对话日志表
CREATE TABLE IF NOT EXISTS ai_conversation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    question TEXT,
    answer TEXT,
    page VARCHAR(255),
    is_transferred INT DEFAULT 0,
    provider VARCHAR(50),
    model VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
