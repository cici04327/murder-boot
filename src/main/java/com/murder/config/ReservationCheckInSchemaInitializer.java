package com.murder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class ReservationCheckInSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ReservationCheckInSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureReservationColumns();
            backfillCheckInData();
            backfillGroupData();
        } catch (Exception e) {
            log.error("初始化预约扩展字段失败", e);
        }
    }

    private void ensureReservationColumns() {
        ensureColumn(
                "check_in_code",
                "ALTER TABLE reservation_order ADD COLUMN check_in_code VARCHAR(20) NULL COMMENT '到店核销码' AFTER refund_status"
        );
        ensureColumn(
                "check_in_status",
                "ALTER TABLE reservation_order ADD COLUMN check_in_status TINYINT DEFAULT 0 COMMENT '到店核销状态：0未核销，1已核销' AFTER check_in_code"
        );
        ensureColumn(
                "check_in_time",
                "ALTER TABLE reservation_order ADD COLUMN check_in_time DATETIME NULL COMMENT '到店核销时间' AFTER check_in_status"
        );
        ensureColumn(
                "group_id",
                "ALTER TABLE reservation_order ADD COLUMN group_id BIGINT NULL COMMENT '关联拼单ID' AFTER script_id"
        );
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("已自动补齐字段 reservation_order.{}", columnName);
    }

    private void backfillCheckInData() {
        jdbcTemplate.update("UPDATE reservation_order SET check_in_status = 0 WHERE check_in_status IS NULL");

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM reservation_order WHERE check_in_code IS NULL OR check_in_code = ''",
                Long.class
        );

        for (Long id : ids) {
            jdbcTemplate.update(
                    "UPDATE reservation_order SET check_in_code = ? WHERE id = ?",
                    generateCheckInCode(),
                    id
            );
        }

        if (!ids.isEmpty()) {
            log.info("已为 {} 条预约补齐核销码", ids.size());
        }
    }

    private void backfillGroupData() {
        jdbcTemplate.update(
                "UPDATE reservation_order ro " +
                        "JOIN group_order go ON go.reservation_id = ro.id " +
                        "SET ro.group_id = go.id " +
                        "WHERE ro.group_id IS NULL"
        );
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
