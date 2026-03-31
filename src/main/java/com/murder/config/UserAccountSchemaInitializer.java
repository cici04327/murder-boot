package com.murder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserAccountSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public UserAccountSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureUserColumns();
            ensureLoginLogTable();
            backfillDefaults();
        } catch (Exception e) {
            log.error("初始化用户设置扩展字段失败", e);
        }
    }

    private void ensureUserColumns() {
        ensureUserColumn(
                "email",
                "ALTER TABLE user ADD COLUMN email VARCHAR(100) NULL COMMENT '邮箱' AFTER phone"
        );
        ensureUserColumn(
                "birthday",
                "ALTER TABLE user ADD COLUMN birthday DATE NULL COMMENT '生日' AFTER email"
        );
        ensureUserColumn(
                "bio",
                "ALTER TABLE user ADD COLUMN bio VARCHAR(500) NULL COMMENT '个人简介' AFTER birthday"
        );
        ensureUserColumn(
                "real_name",
                "ALTER TABLE user ADD COLUMN real_name VARCHAR(50) NULL COMMENT '真实姓名' AFTER bio"
        );
        ensureUserColumn(
                "id_card",
                "ALTER TABLE user ADD COLUMN id_card VARCHAR(32) NULL COMMENT '身份证号' AFTER real_name"
        );
        ensureUserColumn(
                "real_name_verified",
                "ALTER TABLE user ADD COLUMN real_name_verified TINYINT DEFAULT 0 COMMENT '实名认证状态：0未认证，1已认证' AFTER id_card"
        );
        ensureUserColumn(
                "phone_verified",
                "ALTER TABLE user ADD COLUMN phone_verified TINYINT DEFAULT 0 COMMENT '手机验证状态：0未验证，1已验证' AFTER real_name_verified"
        );
        ensureUserColumn(
                "email_verified",
                "ALTER TABLE user ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '邮箱验证状态：0未验证，1已验证' AFTER phone_verified"
        );
        ensureUserColumn(
                "last_login_time",
                "ALTER TABLE user ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间' AFTER email_verified"
        );
    }

    private void ensureUserColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("已自动补齐字段 user.{}", columnName);
    }

    private void ensureLoginLogTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS user_login_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                        "user_id BIGINT NOT NULL COMMENT '用户ID'," +
                        "ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP'," +
                        "location VARCHAR(128) DEFAULT NULL COMMENT '登录地点'," +
                        "device VARCHAR(255) DEFAULT NULL COMMENT '设备信息'," +
                        "status VARCHAR(20) DEFAULT '成功' COMMENT '登录状态'," +
                        "login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间'," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_user_login_log_user_time (user_id, login_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志'"
        );
    }

    private void backfillDefaults() {
        jdbcTemplate.update("UPDATE user SET real_name_verified = 0 WHERE real_name_verified IS NULL");
        jdbcTemplate.update("UPDATE user SET phone_verified = 0 WHERE phone_verified IS NULL");
        jdbcTemplate.update("UPDATE user SET email_verified = 0 WHERE email_verified IS NULL");
    }
}
