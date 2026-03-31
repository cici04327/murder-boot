CREATE TABLE IF NOT EXISTS store_employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL COMMENT '门店ID',
    name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    phone VARCHAR(20) NULL COMMENT '联系电话',
    position INT NULL COMMENT '职位：1店长，2副店长，3主持人，4服务员',
    join_date DATE NULL COMMENT '入职日期',
    salary DECIMAL(10,2) NULL COMMENT '月薪',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1在职，0离职/停用',
    login_account VARCHAR(64) NULL COMMENT '登录账号',
    login_password VARCHAR(64) NULL COMMENT '登录密码（MD5）',
    staff_role VARCHAR(32) NULL COMMENT '员工后台角色：MANAGER/CLERK/DM',
    permission_codes VARCHAR(255) NULL COMMENT '权限编码，逗号分隔',
    dm_id BIGINT NULL COMMENT '绑定DM ID',
    last_login_time DATETIME NULL COMMENT '最后登录时间',
    is_deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_store_id (store_id),
    INDEX idx_login_account (login_account),
    INDEX idx_dm_id (dm_id),
    INDEX idx_status_deleted (status, is_deleted)
) COMMENT='门店员工表';

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 1, '人民广场店长', '13800010001', 1, CURDATE(), 12000.00, 1,
       'manager_1', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_1' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 1, '人民广场店员', '13800010002', 4, CURDATE(), 6500.00, 1,
       'clerk_1', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_1' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 1, '夜枭', '13800010003', 3, CURDATE(), 9000.00, 1,
       'dm_1', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_1' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 2, '虹桥店长', '13800020001', 1, CURDATE(), 12000.00, 1,
       'manager_2', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_2' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 2, '虹桥店员', '13800020002', 4, CURDATE(), 6500.00, 1,
       'clerk_2', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_2' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 2, '柏屿', '13800020003', 3, CURDATE(), 9000.00, 1,
       'dm_2', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 5, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_2' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 3, '松江店长', '13800030001', 1, CURDATE(), 12000.00, 1,
       'manager_3', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_3' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 3, '松江店员', '13800030002', 4, CURDATE(), 6500.00, 1,
       'clerk_3', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_3' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 3, '暗格', '13800030003', 3, CURDATE(), 9000.00, 1,
       'dm_3', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 10, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_3' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 4, '临港店长', '13800040001', 1, CURDATE(), 12000.00, 1,
       'manager_4', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_4' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 4, '临港店员', '13800040002', 4, CURDATE(), 6500.00, 1,
       'clerk_4', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_4' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 4, '司夜', '13800040003', 3, CURDATE(), 9000.00, 1,
       'dm_4', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 14, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_4' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 5, '宝山店长', '13800050001', 1, CURDATE(), 12000.00, 1,
       'manager_5', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_5' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 5, '宝山店员', '13800050002', 4, CURDATE(), 6500.00, 1,
       'clerk_5', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_5' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 5, '陆时', '13800050003', 3, CURDATE(), 9000.00, 1,
       'dm_5', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 19, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_5' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 6, '崇明店长', '13800060001', 1, CURDATE(), 12000.00, 1,
       'manager_6', 'e10adc3949ba59abbe56e057f20f883e', 'MANAGER',
       'reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'manager_6' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 6, '崇明店员', '13800060002', 4, CURDATE(), 6500.00, 1,
       'clerk_6', 'e10adc3949ba59abbe56e057f20f883e', 'CLERK',
       'reservation:view,reservation:checkin', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'clerk_6' AND is_deleted = 0);

INSERT INTO store_employee (
    store_id, name, phone, position, join_date, salary, status,
    login_account, login_password, staff_role, permission_codes, dm_id, is_deleted
)
SELECT 6, '黑雾', '13800060003', 3, CURDATE(), 9000.00, 1,
       'dm_6', 'e10adc3949ba59abbe56e057f20f883e', 'DM',
       'reservation:view,reservation:checkin,reservation:complete', 23, 0
WHERE NOT EXISTS (SELECT 1 FROM store_employee WHERE login_account = 'dm_6' AND is_deleted = 0);
