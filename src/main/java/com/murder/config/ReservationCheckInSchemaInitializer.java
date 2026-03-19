package com.murder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
            backfillFailedGroupSchedules();
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
        jdbcTemplate.update(
                "UPDATE reservation_order SET check_in_code = NULL " +
                        "WHERE (pay_status IS NULL OR pay_status <> 1) AND (check_in_status IS NULL OR check_in_status = 0)"
        );

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM reservation_order " +
                        "WHERE pay_status = 1 AND (check_in_code IS NULL OR check_in_code = '')",
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

    private void backfillFailedGroupSchedules() {
        List<Long> candidateScheduleIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT ro.schedule_id " +
                        "FROM group_order go " +
                        "JOIN reservation_order ro ON (ro.group_id = go.id OR ro.id = go.reservation_id) " +
                        "WHERE go.status = 0 AND ro.schedule_id IS NOT NULL",
                Long.class
        );

        if (candidateScheduleIds == null || candidateScheduleIds.isEmpty()) {
            return;
        }

        Set<Long> updatedScheduleIds = new LinkedHashSet<>();
        for (Long scheduleId : candidateScheduleIds) {
            if (scheduleId == null) {
                continue;
            }

            Integer activePlayers = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(CASE WHEN player_count IS NULL OR player_count <= 0 THEN 1 ELSE player_count END), 0) " +
                            "FROM reservation_order " +
                            "WHERE schedule_id = ? AND is_deleted = 0 AND status <> 4",
                    Integer.class,
                    scheduleId
            );

            Integer requiredPlayers = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(sc.player_count, ss.max_players, 1) " +
                            "FROM script_schedule ss " +
                            "LEFT JOIN script sc ON sc.id = ss.script_id " +
                            "WHERE ss.id = ?",
                    Integer.class,
                    scheduleId
            );

            Integer maxPlayers = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(max_players, 0) FROM script_schedule WHERE id = ?",
                    Integer.class,
                    scheduleId
            );

            Integer activeGroupCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) " +
                            "FROM group_order go " +
                            "JOIN reservation_order ro ON (ro.group_id = go.id OR ro.id = go.reservation_id) " +
                            "WHERE ro.schedule_id = ? " +
                            "AND go.status = 1 " +
                            "AND go.create_time > DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                            "AND (go.play_time IS NULL OR go.play_time > DATE_ADD(NOW(), INTERVAL 2 HOUR))",
                    Integer.class,
                    scheduleId
            );

            int safeActivePlayers = activePlayers != null ? activePlayers : 0;
            int safeRequiredPlayers = requiredPlayers != null && requiredPlayers > 0 ? requiredPlayers : 1;
            int safeMaxPlayers = maxPlayers != null && maxPlayers > 0 ? maxPlayers : safeRequiredPlayers;
            int nextStatus = safeActivePlayers >= safeMaxPlayers ? 0 : 1;

            if ((activeGroupCount == null || activeGroupCount == 0) && safeActivePlayers < safeRequiredPlayers) {
                nextStatus = 2;
            }

            int updated = jdbcTemplate.update(
                    "UPDATE script_schedule " +
                            "SET current_players = ?, status = ?, remark = CASE WHEN ? = 2 THEN COALESCE(NULLIF(remark, ''), '拼团未成团，排期已自动关闭') ELSE remark END, update_time = ? " +
                            "WHERE id = ? AND (current_players <> ? OR status <> ?)",
                    safeActivePlayers,
                    nextStatus,
                    nextStatus,
                    Timestamp.valueOf(LocalDateTime.now()),
                    scheduleId,
                    safeActivePlayers,
                    nextStatus
            );

            if (updated > 0) {
                updatedScheduleIds.add(scheduleId);
            }
        }

        if (!updatedScheduleIds.isEmpty()) {
            log.info("已自动回填 {} 个拼团失败后的排期状态: {}", updatedScheduleIds.size(), updatedScheduleIds);
        }
    }

    private String generateCheckInCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
