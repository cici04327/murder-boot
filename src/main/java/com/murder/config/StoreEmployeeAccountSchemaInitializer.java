package com.murder.config;

import com.murder.entity.Dm;
import com.murder.entity.StoreEmployee;
import com.murder.mapper.DmMapper;
import com.murder.mapper.StoreEmployeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Slf4j
public class StoreEmployeeAccountSchemaInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final JdbcTemplate jdbcTemplate;
    private final StoreEmployeeMapper storeEmployeeMapper;
    private final DmMapper dmMapper;

    public StoreEmployeeAccountSchemaInitializer(JdbcTemplate jdbcTemplate,
                                                 StoreEmployeeMapper storeEmployeeMapper,
                                                 DmMapper dmMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.storeEmployeeMapper = storeEmployeeMapper;
        this.dmMapper = dmMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureBaseTable();
            ensureColumns();
            backfillEmployeeAccounts();
        } catch (Exception e) {
            log.error("初始化门店员工账号扩展字段失败", e);
        }
    }

    private void ensureBaseTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS store_employee (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "store_id BIGINT NOT NULL COMMENT '门店ID'," +
                        "name VARCHAR(64) NOT NULL COMMENT '员工姓名'," +
                        "phone VARCHAR(20) NULL COMMENT '联系电话'," +
                        "position INT NULL COMMENT '职位：1店长，2副店长，3主持人，4服务员'," +
                        "join_date DATE NULL COMMENT '入职日期'," +
                        "salary DECIMAL(10,2) NULL COMMENT '月薪'," +
                        "status INT NOT NULL DEFAULT 1 COMMENT '状态：1在职，0离职/停用'," +
                        "login_account VARCHAR(64) NULL COMMENT '登录账号'," +
                        "login_password VARCHAR(64) NULL COMMENT '登录密码（MD5）'," +
                        "staff_role VARCHAR(32) NULL COMMENT '员工后台角色：MANAGER/CLERK/DM'," +
                        "permission_codes VARCHAR(255) NULL COMMENT '权限编码，逗号分隔'," +
                        "dm_id BIGINT NULL COMMENT '绑定DM ID'," +
                        "last_login_time DATETIME NULL COMMENT '最后登录时间'," +
                        "is_deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除'," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                        "INDEX idx_store_id (store_id)," +
                        "INDEX idx_login_account (login_account)," +
                        "INDEX idx_dm_id (dm_id)," +
                        "INDEX idx_status_deleted (status, is_deleted)" +
                        ") COMMENT='门店员工表'"
        );
    }

    private void ensureColumns() {
        ensureColumn(
                "login_account",
                "ALTER TABLE store_employee ADD COLUMN login_account VARCHAR(64) NULL COMMENT '登录账号' AFTER status"
        );
        ensureColumn(
                "login_password",
                "ALTER TABLE store_employee ADD COLUMN login_password VARCHAR(64) NULL COMMENT '登录密码（MD5）' AFTER login_account"
        );
        ensureColumn(
                "staff_role",
                "ALTER TABLE store_employee ADD COLUMN staff_role VARCHAR(32) NULL COMMENT '员工后台角色：MANAGER/CLERK/DM' AFTER login_password"
        );
        ensureColumn(
                "permission_codes",
                "ALTER TABLE store_employee ADD COLUMN permission_codes VARCHAR(255) NULL COMMENT '权限编码，逗号分隔' AFTER staff_role"
        );
        ensureColumn(
                "dm_id",
                "ALTER TABLE store_employee ADD COLUMN dm_id BIGINT NULL COMMENT '绑定DM ID' AFTER permission_codes"
        );
        ensureColumn(
                "last_login_time",
                "ALTER TABLE store_employee ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间' AFTER dm_id"
        );
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS store_employee_operation_log (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "store_id BIGINT NULL COMMENT '门店ID'," +
                        "operator_id BIGINT NULL COMMENT '操作人ID'," +
                        "employee_id BIGINT NULL COMMENT '员工ID'," +
                        "operator_type VARCHAR(32) NULL COMMENT '操作人类型'," +
                        "operator_role VARCHAR(64) NULL COMMENT '操作人角色'," +
                        "operator_name VARCHAR(64) NULL COMMENT '操作人名称'," +
                        "action_type VARCHAR(64) NOT NULL COMMENT '操作类型'," +
                        "target_type VARCHAR(64) NULL COMMENT '目标类型'," +
                        "target_id BIGINT NULL COMMENT '目标ID'," +
                        "target_name VARCHAR(255) NULL COMMENT '目标名称'," +
                        "detail VARCHAR(500) NULL COMMENT '操作详情'," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                        "INDEX idx_store_create_time (store_id, create_time)," +
                        "INDEX idx_employee_create_time (employee_id, create_time)," +
                        "INDEX idx_action_type (action_type)" +
                        ") COMMENT='门店员工操作日志表'"
        );
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'store_employee' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("已自动补齐字段 store_employee.{}", columnName);
    }

    private void backfillEmployeeAccounts() {
        List<StoreEmployee> employees = storeEmployeeMapper.selectList(null);
        if (employees == null || employees.isEmpty()) {
            return;
        }

        String encodedDefaultPassword = DigestUtils.md5DigestAsHex(DEFAULT_PASSWORD.getBytes());
        int updatedCount = 0;
        for (StoreEmployee employee : employees) {
            boolean needUpdate = false;

            if (!StringUtils.hasText(employee.getStaffRole())) {
                employee.setStaffRole(resolveStaffRole(employee.getPosition()));
                needUpdate = true;
            }

            if (!StringUtils.hasText(employee.getPermissionCodes())) {
                employee.setPermissionCodes(resolvePermissionCodes(employee.getStaffRole()));
                needUpdate = true;
            } else if (isLegacyPermissionCodes(employee.getStaffRole(), employee.getPermissionCodes())) {
                employee.setPermissionCodes(resolvePermissionCodes(employee.getStaffRole()));
                needUpdate = true;
            }

            if (!StringUtils.hasText(employee.getLoginAccount())
                    && StringUtils.hasText(employee.getPhone())
                    && isUniqueLoginAccount(employee.getPhone(), employee.getId())) {
                employee.setLoginAccount(employee.getPhone());
                needUpdate = true;
            }

            if (!StringUtils.hasText(employee.getLoginPassword()) && StringUtils.hasText(employee.getLoginAccount())) {
                employee.setLoginPassword(encodedDefaultPassword);
                needUpdate = true;
            }

            if ("DM".equals(employee.getStaffRole()) && employee.getDmId() == null) {
                Dm dm = findDmByStoreAndName(employee.getStoreId(), employee.getName());
                if (dm != null) {
                    employee.setDmId(dm.getId());
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                storeEmployeeMapper.updateById(employee);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("已自动回填 {} 个门店员工后台账号字段，默认密码为 {}", updatedCount, DEFAULT_PASSWORD);
        }
    }

    private String resolveStaffRole(Integer position) {
        if (position == null) {
            return "CLERK";
        }
        if (position == 1 || position == 2) {
            return "MANAGER";
        }
        if (position == 3) {
            return "DM";
        }
        return "CLERK";
    }

    private String resolvePermissionCodes(String staffRole) {
        if ("MANAGER".equals(staffRole)) {
            return "reservation:view,reservation:checkin,reservation:complete,reservation:assign_dm,refund:process,employee:manage,report:view,notification:view";
        }
        if ("DM".equals(staffRole)) {
            return "reservation:view,reservation:complete,notification:view";
        }
        return "reservation:view,reservation:checkin,notification:view";
    }

    private boolean isLegacyPermissionCodes(String staffRole, String permissionCodes) {
        java.util.Set<String> currentCodes = java.util.Arrays.stream(String.valueOf(permissionCodes).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
        if (currentCodes.isEmpty()) {
            return false;
        }
        if ("MANAGER".equals(staffRole)) {
            return currentCodes.equals(java.util.Set.of(
                    "reservation:view",
                    "reservation:checkin",
                    "reservation:complete",
                    "reservation:assign_dm",
                    "refund:process"
            ));
        }
        if ("CLERK".equals(staffRole)) {
            return currentCodes.equals(java.util.Set.of(
                    "reservation:view",
                    "reservation:checkin"
            ));
        }
        if ("DM".equals(staffRole)) {
            return currentCodes.equals(java.util.Set.of(
                    "reservation:view",
                    "reservation:checkin",
                    "reservation:complete"
            ));
        }
        return false;
    }

    private Dm findDmByStoreAndName(Long storeId, String employeeName) {
        if (storeId == null || !StringUtils.hasText(employeeName)) {
            return null;
        }
        return dmMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dm>()
                .eq(Dm::getStoreId, storeId)
                .eq(Dm::getName, employeeName)
                .eq(Dm::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private boolean isUniqueLoginAccount(String loginAccount, Long currentEmployeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM store_employee WHERE login_account = ? AND is_deleted = 0 AND (? IS NULL OR id <> ?)",
                Integer.class,
                loginAccount,
                currentEmployeeId,
                currentEmployeeId
        );
        return count == null || count == 0;
    }
}
