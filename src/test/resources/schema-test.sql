-- 测试数据库初始化脚本

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(255),
    gender INT DEFAULT 0,
    birthday DATE,
    status INT DEFAULT 1,
    role INT DEFAULT 0,
    points INT DEFAULT 0,
    vip_level INT DEFAULT 0,
    vip_expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 门店表
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
    opening_time TIME,
    closing_time TIME,
    rating DECIMAL(2, 1) DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 剧本分类表
CREATE TABLE IF NOT EXISTS script_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 剧本表
CREATE TABLE IF NOT EXISTS script (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT,
    cover VARCHAR(255),
    description TEXT,
    type INT DEFAULT 1,
    difficulty INT DEFAULT 1,
    player_count INT DEFAULT 6,
    duration DECIMAL(3, 1) DEFAULT 4.0,
    price DECIMAL(10, 2),
    rating DECIMAL(2, 1) DEFAULT 0,
    play_count INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 门店房间表
CREATE TABLE IF NOT EXISTS store_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    -- 房间类型：1小房 2中房 3大房（管理端/预约端会使用）
    type INT DEFAULT 1,
    capacity INT DEFAULT 10,
    price DECIMAL(10, 2),
    description VARCHAR(255),
    status INT DEFAULT 1,
    -- 逻辑删除：0未删除 1已删除（与 @TableLogic 对齐）
    is_deleted INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 预约表
CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    room_id BIGINT,
    script_id BIGINT,
    reservation_time DATETIME NOT NULL,
    duration DECIMAL(3, 1) DEFAULT 3.0,
    player_count INT DEFAULT 1,
    total_price DECIMAL(10, 2),
    status INT DEFAULT 0,
    pay_status INT DEFAULT 0,
    pay_time DATETIME,
    check_in_code VARCHAR(20),
    check_in_status INT DEFAULT 0,
    check_in_time DATETIME,
    cancel_reason VARCHAR(255),
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 优惠券表
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type INT DEFAULT 1,
    value DECIMAL(10, 2),
    min_amount DECIMAL(10, 2) DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    total_count INT DEFAULT 0,
    used_count INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status INT DEFAULT 0,
    use_time DATETIME,
    order_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
