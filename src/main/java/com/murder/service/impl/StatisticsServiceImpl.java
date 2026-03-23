package com.murder.service.impl;

import com.murder.vo.StatisticsChartsVO;
import com.murder.vo.StatisticsOverviewVO;
import com.murder.vo.StatisticsRankingVO;
import com.murder.vo.StatisticsRealtimeVO;
import com.murder.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现�?
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public StatisticsOverviewVO getOverview() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        
        // 今日营业额：仅统计今日创建且已支付订单
        String sql = "SELECT COALESCE(SUM(actual_amount), 0) FROM reservation_order " +
                "WHERE create_time >= ? AND create_time < ? AND pay_status = 1 AND is_deleted = 0";
        BigDecimal todayRevenue = jdbcTemplate.queryForObject(sql, BigDecimal.class, todayStart, tomorrowStart);
        
        // 昨日营业额：严格限制在昨日自然日区间内
        BigDecimal yesterdayRevenue = jdbcTemplate.queryForObject(sql, BigDecimal.class, yesterdayStart, todayStart);
        
        // 今日预约数
        sql = "SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND create_time < ? AND is_deleted = 0";
        Integer todayReservations = jdbcTemplate.queryForObject(sql, Integer.class, todayStart, tomorrowStart);
        
        // 昨日预约数：严格限制在昨日自然日区间内
        Integer yesterdayReservations = jdbcTemplate.queryForObject(sql, Integer.class, yesterdayStart, todayStart);
        
        // 今日新增用户
        sql = "SELECT COUNT(*) FROM user WHERE create_time >= ? AND create_time < ? AND is_deleted = 0";
        Integer todayNewUsers = jdbcTemplate.queryForObject(sql, Integer.class, todayStart, tomorrowStart);
        
        // 累计用户�?
        sql = "SELECT COUNT(*) FROM user WHERE is_deleted = 0";
        Integer totalUsers = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 在线门店�?
        sql = "SELECT COUNT(*) FROM store WHERE status = 1 AND is_deleted = 0";
        Integer onlineStores = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 总门店数
        sql = "SELECT COUNT(*) FROM store WHERE is_deleted = 0";
        Integer totalStores = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 今日优惠券使用数（use_time 可能为 null，用 update_time 兜底）
        Integer todayCouponUsed;
        try {
            sql = "SELECT COUNT(*) FROM user_coupon WHERE use_time >= ? AND use_time < ? AND status = 2";
            todayCouponUsed = jdbcTemplate.queryForObject(sql, Integer.class, todayStart, tomorrowStart);
        } catch (Exception e) {
            log.warn("查询优惠券使用数失败（字段不存在），使用状态字段兜底: {}", e.getMessage());
            try {
                sql = "SELECT COUNT(*) FROM user_coupon WHERE status = 2";
                todayCouponUsed = jdbcTemplate.queryForObject(sql, Integer.class);
            } catch (Exception e2) {
                todayCouponUsed = 0;
            }
        }
        
        // 优惠券使用率（今日领取数，字段名兼容 receive_time / create_time）
        Integer todayCouponReceived;
        try {
            sql = "SELECT COUNT(*) FROM user_coupon WHERE create_time >= ? AND create_time < ?";
            todayCouponReceived = jdbcTemplate.queryForObject(sql, Integer.class, todayStart, tomorrowStart);
        } catch (Exception e) {
            log.warn("查询优惠券领取数失败: {}", e.getMessage());
            todayCouponReceived = 0;
        }
        BigDecimal couponUsageRate = BigDecimal.ZERO;
        if (todayCouponReceived != null && todayCouponReceived > 0) {
            couponUsageRate = BigDecimal.valueOf(todayCouponUsed)
                    .divide(BigDecimal.valueOf(todayCouponReceived), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // 精选剧本数（状态为1的剧本）
        sql = "SELECT COUNT(*) FROM script WHERE status = 1 AND is_deleted = 0";
        Integer totalScripts;
        try {
            totalScripts = jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            log.warn("查询剧本数失败: {}", e.getMessage());
            totalScripts = 0;
        }
        
        // 用户满意度（安全查询，表不存在时兜底）
        BigDecimal satisfactionRate = BigDecimal.valueOf(95);
        try {
            sql = "SELECT AVG(rating) FROM script_review WHERE is_deleted = 0";
            BigDecimal avgRating = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
                satisfactionRate = avgRating.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                sql = "SELECT AVG(rating) FROM store_review WHERE is_deleted = 0";
                BigDecimal storeAvgRating = jdbcTemplate.queryForObject(sql, BigDecimal.class);
                if (storeAvgRating != null && storeAvgRating.compareTo(BigDecimal.ZERO) > 0) {
                    satisfactionRate = storeAvgRating.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
            }
        } catch (Exception e) {
            log.warn("查询满意度失败（评价表可能不存在），使用默认值95%: {}", e.getMessage());
        }
        
        // 计算增长率
        BigDecimal revenueGrowth = calculateGrowth(todayRevenue, yesterdayRevenue);
        BigDecimal reservationGrowth = calculateGrowth(
                BigDecimal.valueOf(todayReservations), 
                BigDecimal.valueOf(yesterdayReservations)
        );
        
        return StatisticsOverviewVO.builder()
                .todayRevenue(todayRevenue)
                .revenueGrowth(revenueGrowth)
                .todayReservations(todayReservations)
                .reservationGrowth(reservationGrowth)
                .todayNewUsers(todayNewUsers)
                .totalUsers(totalUsers)
                .onlineStores(onlineStores)
                .totalStores(totalStores)
                .todayCouponUsed(todayCouponUsed)
                .couponUsageRate(couponUsageRate)
                .yesterdayRevenue(yesterdayRevenue)
                .yesterdayReservations(yesterdayReservations)
                .totalScripts(totalScripts)
                .satisfactionRate(satisfactionRate)
                .build();
    }

    @Override
    public StatisticsChartsVO getCharts(Integer days) {
        if (days == null) days = 7;
        
        LocalDateTime endDate = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime startDate = endDate.minusDays(days - 1).with(LocalTime.MIN);
        
        // 营业额趋�?
        String sql = "SELECT DATE(create_time) as date, COALESCE(SUM(actual_amount), 0) as amount " +
                "FROM reservation_order " +
                "WHERE create_time BETWEEN ? AND ? AND status IN (1,2,3) AND is_deleted = 0 " +
                "GROUP BY DATE(create_time) ORDER BY date";
        
        List<Map<String, Object>> revenueData = jdbcTemplate.queryForList(sql, startDate, endDate);
        List<String> revenueDates = new ArrayList<>();
        List<BigDecimal> revenueAmounts = new ArrayList<>();
        
        for (Map<String, Object> row : revenueData) {
            revenueDates.add(row.get("date").toString());
            revenueAmounts.add((BigDecimal) row.get("amount"));
        }
        
        // 预约来源分布（模拟数据，实际需要在数据库表中添加source字段�?
        Map<String, Integer> reservationSource = new LinkedHashMap<>();
        reservationSource.put("线上预约", 65);
        reservationSource.put("电话预约", 25);
        reservationSource.put("到店预约", 10);
        
        // 用户增长趋势
        sql = "SELECT DATE(create_time) as date, COUNT(*) as count " +
                "FROM user " +
                "WHERE create_time BETWEEN ? AND ? AND is_deleted = 0 " +
                "GROUP BY DATE(create_time) ORDER BY date";
        
        List<Map<String, Object>> userGrowthData = jdbcTemplate.queryForList(sql, startDate, endDate);
        List<String> userGrowthDates = new ArrayList<>();
        List<Integer> userGrowthCounts = new ArrayList<>();
        
        for (Map<String, Object> row : userGrowthData) {
            userGrowthDates.add(row.get("date").toString());
            userGrowthCounts.add(((Number) row.get("count")).intValue());
        }
        
        // 会员等级分布
        sql = "SELECT member_level, COUNT(*) as count FROM user WHERE is_deleted = 0 GROUP BY member_level";
        List<Map<String, Object>> memberData = jdbcTemplate.queryForList(sql);
        Map<String, Integer> memberLevelDistribution = new LinkedHashMap<>();
        
        String[] levelNames = {"", "青铜会员", "白银会员", "黄金会员", "钻石会员"};
        for (Map<String, Object> row : memberData) {
            int level = ((Number) row.get("member_level")).intValue();
            int count = ((Number) row.get("count")).intValue();
            if (level > 0 && level < levelNames.length) {
                memberLevelDistribution.put(levelNames[level], count);
            }
        }
        
        return StatisticsChartsVO.builder()
                .revenueDates(revenueDates)
                .revenueAmounts(revenueAmounts)
                .reservationSource(reservationSource)
                .userGrowthDates(userGrowthDates)
                .userGrowthCounts(userGrowthCounts)
                .memberLevelDistribution(memberLevelDistribution)
                .build();
    }

    @Override
    public StatisticsRankingVO getRankings(Integer limit) {
        if (limit == null) limit = 10;
        
        // 门店排行
        String sql = "SELECT s.id, s.name, COALESCE(SUM(r.actual_amount), 0) as revenue, COUNT(r.id) as count " +
                "FROM store s " +
                "LEFT JOIN reservation_order r ON s.id = r.store_id AND r.is_deleted = 0 AND r.status IN (1,2,3) " +
                "WHERE s.is_deleted = 0 " +
                "GROUP BY s.id, s.name " +
                "ORDER BY revenue DESC " +
                "LIMIT ?";
        
        List<Map<String, Object>> storeData = jdbcTemplate.queryForList(sql, limit);
        List<StatisticsRankingVO.StoreRankItem> storeRankings = new ArrayList<>();
        
        for (Map<String, Object> row : storeData) {
            storeRankings.add(StatisticsRankingVO.StoreRankItem.builder()
                    .id(((Number) row.get("id")).longValue())
                    .name((String) row.get("name"))
                    .revenue((BigDecimal) row.get("revenue"))
                    .reservationCount(((Number) row.get("count")).intValue())
                    .build());
        }
        
        // 剧本排行
        sql = "SELECT sc.id, sc.name, COUNT(r.id) as count, sc.rating " +
                "FROM script sc " +
                "LEFT JOIN reservation_order r ON sc.id = r.script_id AND r.is_deleted = 0 " +
                "WHERE sc.is_deleted = 0 " +
                "GROUP BY sc.id, sc.name, sc.rating " +
                "ORDER BY count DESC " +
                "LIMIT ?";
        
        List<Map<String, Object>> scriptData = jdbcTemplate.queryForList(sql, limit);
        List<StatisticsRankingVO.ScriptRankItem> scriptRankings = new ArrayList<>();
        
        for (Map<String, Object> row : scriptData) {
            scriptRankings.add(StatisticsRankingVO.ScriptRankItem.builder()
                    .id(((Number) row.get("id")).longValue())
                    .name((String) row.get("name"))
                    .bookingCount(((Number) row.get("count")).intValue())
                    .rating((BigDecimal) row.get("rating"))
                    .build());
        }
        
        return StatisticsRankingVO.builder()
                .storeRankings(storeRankings)
                .scriptRankings(scriptRankings)
                .build();
    }

    @Override
    public StatisticsRealtimeVO getRealtime(Integer limit) {
        if (limit == null) limit = 10;
        
        // 最新预约动�?
        String sql = "SELECT u.nickname as user_nickname, sc.name as script_name, " +
                "st.name as store_name, r.reservation_time " +
                "FROM reservation_order r " +
                "LEFT JOIN user u ON r.user_id = u.id " +
                "LEFT JOIN script sc ON r.script_id = sc.id " +
                "LEFT JOIN store st ON r.store_id = st.id " +
                "WHERE r.is_deleted = 0 " +
                "ORDER BY r.create_time DESC " +
                "LIMIT ?";
        
        List<Map<String, Object>> reservationData = jdbcTemplate.queryForList(sql, limit);
        List<StatisticsRealtimeVO.RecentReservation> recentReservations = new ArrayList<>();
        
        for (Map<String, Object> row : reservationData) {
            recentReservations.add(StatisticsRealtimeVO.RecentReservation.builder()
                    .userNickname((String) row.get("user_nickname"))
                    .scriptName((String) row.get("script_name"))
                    .storeName((String) row.get("store_name"))
                    .reservationTime((LocalDateTime) row.get("reservation_time"))
                    .build());
        }
        
        // 今日热门剧本TOP5
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        sql = "SELECT sc.name, COUNT(r.id) as count " +
                "FROM reservation_order r " +
                "JOIN script sc ON r.script_id = sc.id " +
                "WHERE r.create_time >= ? AND r.is_deleted = 0 " +
                "GROUP BY sc.id, sc.name " +
                "ORDER BY count DESC " +
                "LIMIT 5";
        
        List<Map<String, Object>> hotScriptData = jdbcTemplate.queryForList(sql, todayStart);
        List<StatisticsRealtimeVO.HotScript> hotScripts = new ArrayList<>();
        
        for (Map<String, Object> row : hotScriptData) {
            hotScripts.add(StatisticsRealtimeVO.HotScript.builder()
                    .name((String) row.get("name"))
                    .todayCount(((Number) row.get("count")).intValue())
                    .build());
        }
        
        // 最近优惠券使用
        sql = "SELECT u.nickname as user_nickname, c.name as coupon_name, uc.use_time " +
                "FROM user_coupon uc " +
                "JOIN user u ON uc.user_id = u.id " +
                "JOIN coupon c ON uc.coupon_id = c.id " +
                "WHERE uc.status = 2 " +
                "ORDER BY uc.use_time DESC " +
                "LIMIT ?";
        
        List<Map<String, Object>> couponData = jdbcTemplate.queryForList(sql, limit);
        List<StatisticsRealtimeVO.RecentCouponUse> recentCouponUses = new ArrayList<>();
        
        for (Map<String, Object> row : couponData) {
            recentCouponUses.add(StatisticsRealtimeVO.RecentCouponUse.builder()
                    .userNickname((String) row.get("user_nickname"))
                    .couponName((String) row.get("coupon_name"))
                    .useTime((LocalDateTime) row.get("use_time"))
                    .build());
        }
        
        return StatisticsRealtimeVO.builder()
                .recentReservations(recentReservations)
                .hotScripts(hotScripts)
                .recentCouponUses(recentCouponUses)
                .build();
    }

    /**
     * 计算增长�?
     */
    @Override
    public Map<String, Object> getOperationBoard(Integer days, Long storeId) {
        if (days == null || days <= 0) days = 30;
        LocalDateTime start = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        String storeWhere = storeId != null ? " AND store_id = " + storeId : "";

        // 1. 预约转化率 = 已支付 / 总创建
        try {
            int total  = safeQueryInt("SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0" + storeWhere, start, end);
            int paid   = safeQueryInt("SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere, start, end);
            result.put("totalReservations", total);
            result.put("paidReservations", paid);
            result.put("conversionRate", total > 0 ? Math.round(paid * 10000.0 / total) / 100.0 : 0);
        } catch (Exception e) { log.warn("转化率统计失败: {}", e.getMessage()); result.put("conversionRate", 0); }

        // 2. 取消率
        try {
            int total  = ((Number) result.getOrDefault("totalReservations", 0)).intValue();
            int cancel = safeQueryInt("SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND status = 4" + storeWhere, start, end);
            result.put("cancelReservations", cancel);
            result.put("cancelRate", total > 0 ? Math.round(cancel * 10000.0 / total) / 100.0 : 0);
        } catch (Exception e) { log.warn("取消率统计失败: {}", e.getMessage()); result.put("cancelRate", 0); }

        // 3. 退款率
        try {
            int total  = ((Number) result.getOrDefault("totalReservations", 0)).intValue();
            int refund = safeQueryInt("SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND refund_status=2" + storeWhere, start, end);
            result.put("refundCount", refund);
            result.put("refundRate", total > 0 ? Math.round(refund * 10000.0 / total) / 100.0 : 0);
        } catch (Exception e) { log.warn("退款率统计失败: {}", e.getMessage()); result.put("refundRate", 0); }

        // 4. 复购率
        try {
            String repSql   = "SELECT COUNT(*) FROM (SELECT user_id FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere + " GROUP BY user_id HAVING COUNT(*)>=2) t";
            String userSql  = "SELECT COUNT(DISTINCT user_id) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere;
            int repUsers    = safeQueryInt(repSql, start, end);
            int totalUsers  = safeQueryInt(userSql, start, end);
            result.put("repurchaseUsers", repUsers);
            result.put("totalPayingUsers", totalUsers);
            result.put("repurchaseRate", totalUsers > 0 ? Math.round(repUsers * 10000.0 / totalUsers) / 100.0 : 0);
        } catch (Exception e) { log.warn("复购率统计失败: {}", e.getMessage()); result.put("repurchaseRate", 0); }

        // 5. 房间利用率
        try {
            LocalDate sd = start.toLocalDate(), ed = end.toLocalDate();
            String storeW2 = storeId != null ? " AND store_id=" + storeId : "";
            int total = safeQueryInt("SELECT COUNT(*) FROM script_schedule WHERE schedule_date >= ? AND schedule_date <= ? AND is_deleted=0" + storeW2, sd, ed);
            int used  = safeQueryInt("SELECT COUNT(*) FROM script_schedule WHERE schedule_date >= ? AND schedule_date <= ? AND is_deleted=0 AND current_players > 0" + storeW2, sd, ed);
            result.put("scheduleTotal", total);
            result.put("scheduleUsed", used);
            result.put("roomUtilizationRate", total > 0 ? Math.round(used * 10000.0 / total) / 100.0 : 0);
        } catch (Exception e) { log.warn("房间利用率统计失败: {}", e.getMessage()); result.put("roomUtilizationRate", 0); }

        // 6. 平均客单价
        try {
            BigDecimal avg = jdbcTemplate.queryForObject("SELECT AVG(actual_amount) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere, BigDecimal.class, start, end);
            result.put("avgOrderAmount", avg != null ? avg.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        } catch (Exception e) { log.warn("客单价统计失败: {}", e.getMessage()); result.put("avgOrderAmount", BigDecimal.ZERO); }

        // 7. 总营收
        try {
            BigDecimal rev = jdbcTemplate.queryForObject("SELECT SUM(actual_amount) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere, BigDecimal.class, start, end);
            result.put("totalRevenue", rev != null ? rev.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        } catch (Exception e) { log.warn("总营收统计失败: {}", e.getMessage()); result.put("totalRevenue", BigDecimal.ZERO); }

        // 8. 每日趋势
        try {
            String trendSql = "SELECT DATE(create_time) as d, COUNT(*) as cnt, SUM(CASE WHEN pay_status=1 THEN actual_amount ELSE 0 END) as rev " +
                    "FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0" + storeWhere + " GROUP BY DATE(create_time) ORDER BY d";
            List<Map<String, Object>> trend = jdbcTemplate.queryForList(trendSql, start, end);
            List<String> dates = new java.util.ArrayList<>();
            List<Integer> counts = new java.util.ArrayList<>();
            List<BigDecimal> revenues = new java.util.ArrayList<>();
            for (Map<String, Object> row : trend) {
                dates.add(String.valueOf(row.get("d")));
                counts.add(((Number) row.get("cnt")).intValue());
                Object rev = row.get("rev");
                revenues.add(rev != null ? new BigDecimal(rev.toString()).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
            }
            result.put("trendDates", dates);
            result.put("trendCounts", counts);
            result.put("trendRevenues", revenues);
        } catch (Exception e) { log.warn("趋势统计失败: {}", e.getMessage()); }

        // 9. 剧本热度榜 TOP8
        try {
            List<Map<String, Object>> rank = jdbcTemplate.queryForList(
                "SELECT s.name, COUNT(r.id) as cnt FROM reservation_order r LEFT JOIN script s ON r.script_id=s.id " +
                "WHERE r.create_time >= ? AND r.create_time <= ? AND r.is_deleted=0 AND r.pay_status=1" + storeWhere +
                " GROUP BY r.script_id, s.name ORDER BY cnt DESC LIMIT 8", start, end);
            result.put("scriptHotRank", rank);
        } catch (Exception e) { log.warn("剧本热度统计失败: {}", e.getMessage()); result.put("scriptHotRank", new java.util.ArrayList<>()); }

        // 10. 新老客户分层
        try {
            String newSql = "SELECT COUNT(DISTINCT user_id) FROM reservation_order WHERE create_time >= ? AND create_time <= ? AND is_deleted=0 AND pay_status=1" + storeWhere +
                    " AND user_id NOT IN (SELECT DISTINCT user_id FROM reservation_order WHERE create_time < ? AND is_deleted=0 AND pay_status=1" + storeWhere + ")";
            int newCustomers = safeQueryInt(newSql, start, end, start);
            int total = ((Number) result.getOrDefault("totalPayingUsers", 0)).intValue();
            result.put("newCustomers", newCustomers);
            result.put("returningCustomers", Math.max(0, total - newCustomers));
        } catch (Exception e) { log.warn("用户分层统计失败: {}", e.getMessage()); }

        result.put("days", days);
        result.put("storeId", storeId);
        result.put("generatedAt", LocalDateTime.now().toString());
        return result;
    }

    @Override
    public Map<String, Object> getStoreDailyReport(Long storeId) {
        Map<String, Object> result = new HashMap<>();
        String sw = storeId != null ? " AND store_id = " + storeId : "";

        LocalDateTime todayStart     = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime weekStart      = todayStart.minusDays(todayStart.getDayOfWeek().getValue() - 1);
        LocalDateTime monthStart     = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        // ── 今日 ──
        String revSql = "SELECT COALESCE(SUM(actual_amount),0) FROM reservation_order WHERE create_time>=? AND create_time<? AND pay_status=1 AND is_deleted=0" + sw;
        String cntSql = "SELECT COUNT(*) FROM reservation_order WHERE create_time>=? AND create_time<? AND is_deleted=0" + sw;
        String paidSql= "SELECT COUNT(*) FROM reservation_order WHERE create_time>=? AND create_time<? AND pay_status=1 AND is_deleted=0" + sw;

        BigDecimal todayRevenue    = safeQueryBig(revSql,  todayStart, todayStart.plusDays(1));
        BigDecimal yesterdayRevenue= safeQueryBig(revSql,  yesterdayStart, todayStart);
        BigDecimal weekRevenue     = safeQueryBig(revSql,  weekStart,  todayStart.plusDays(1));
        BigDecimal monthRevenue    = safeQueryBig(revSql,  monthStart, todayStart.plusDays(1));

        int todayCnt    = safeQueryInt(cntSql,  todayStart, todayStart.plusDays(1));
        int yesterdayCnt= safeQueryInt(cntSql,  yesterdayStart, todayStart);
        int weekCnt     = safeQueryInt(cntSql,  weekStart,  todayStart.plusDays(1));
        int monthCnt    = safeQueryInt(cntSql,  monthStart, todayStart.plusDays(1));

        int todayPaid   = safeQueryInt(paidSql, todayStart, todayStart.plusDays(1));
        int weekPaid    = safeQueryInt(paidSql, weekStart,  todayStart.plusDays(1));
        int monthPaid   = safeQueryInt(paidSql, monthStart, todayStart.plusDays(1));

        // 环比增长率（今日 vs 昨日）
        BigDecimal revenueGrowth     = calculateGrowth(todayRevenue, yesterdayRevenue);
        BigDecimal reservationGrowth = calculateGrowth(BigDecimal.valueOf(todayCnt), BigDecimal.valueOf(yesterdayCnt));

        // 平均客单价
        BigDecimal todayAvg = todayPaid > 0
                ? todayRevenue.divide(BigDecimal.valueOf(todayPaid), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 今日各小时预约分布（热力图数据）
        List<Map<String, Object>> hourlyDist = new ArrayList<>();
        try {
            String hourSql = "SELECT HOUR(create_time) as h, COUNT(*) as cnt " +
                    "FROM reservation_order WHERE create_time>=? AND create_time<? AND is_deleted=0" + sw +
                    " GROUP BY HOUR(create_time) ORDER BY h";
            hourlyDist = jdbcTemplate.queryForList(hourSql, todayStart, todayStart.plusDays(1));
        } catch (Exception e) { log.warn("小时分布查询失败: {}", e.getMessage()); }

        // 本周每日营收趋势
        List<String> weekDates   = new ArrayList<>();
        List<BigDecimal> weekRevs= new ArrayList<>();
        List<Integer> weekCnts   = new ArrayList<>();
        try {
            String weekTrendSql = "SELECT DATE(create_time) as d, " +
                    "SUM(CASE WHEN pay_status=1 THEN actual_amount ELSE 0 END) as rev, COUNT(*) as cnt " +
                    "FROM reservation_order WHERE create_time>=? AND create_time<? AND is_deleted=0" + sw +
                    " GROUP BY DATE(create_time) ORDER BY d";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(weekTrendSql, weekStart, todayStart.plusDays(1));
            for (Map<String, Object> row : rows) {
                weekDates.add(String.valueOf(row.get("d")));
                Object rev = row.get("rev");
                weekRevs.add(rev != null ? new BigDecimal(rev.toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                weekCnts.add(((Number) row.get("cnt")).intValue());
            }
        } catch (Exception e) { log.warn("周趋势查询失败: {}", e.getMessage()); }

        // 今日剧本热度 TOP5
        List<Map<String, Object>> topScripts = new ArrayList<>();
        try {
            String topSql = "SELECT s.name as scriptName, COUNT(r.id) as cnt, " +
                    "SUM(CASE WHEN r.pay_status=1 THEN r.actual_amount ELSE 0 END) as rev " +
                    "FROM reservation_order r LEFT JOIN script s ON r.script_id=s.id " +
                    "WHERE r.create_time>=? AND r.create_time<? AND r.is_deleted=0" + sw +
                    " GROUP BY r.script_id, s.name ORDER BY cnt DESC LIMIT 5";
            topScripts = jdbcTemplate.queryForList(topSql, todayStart, todayStart.plusDays(1));
        } catch (Exception e) { log.warn("剧本热度查询失败: {}", e.getMessage()); }

        // 今日退款/取消数
        int todayRefund = safeQueryInt(
                "SELECT COUNT(*) FROM reservation_order WHERE create_time>=? AND create_time<? AND refund_status=2 AND is_deleted=0" + sw,
                todayStart, todayStart.plusDays(1));
        int todayCancel = safeQueryInt(
                "SELECT COUNT(*) FROM reservation_order WHERE create_time>=? AND create_time<? AND status IN(3,4) AND is_deleted=0" + sw,
                todayStart, todayStart.plusDays(1));

        result.put("todayRevenue",      todayRevenue);
        result.put("yesterdayRevenue",  yesterdayRevenue);
        result.put("weekRevenue",       weekRevenue);
        result.put("monthRevenue",      monthRevenue);
        result.put("revenueGrowth",     revenueGrowth);
        result.put("todayReservations", todayCnt);
        result.put("yesterdayReservations", yesterdayCnt);
        result.put("weekReservations",  weekCnt);
        result.put("monthReservations", monthCnt);
        result.put("reservationGrowth", reservationGrowth);
        result.put("todayPaid",         todayPaid);
        result.put("weekPaid",          weekPaid);
        result.put("monthPaid",         monthPaid);
        result.put("todayAvgAmount",    todayAvg);
        result.put("todayRefund",       todayRefund);
        result.put("todayCancel",       todayCancel);
        result.put("hourlyDistribution",hourlyDist);
        result.put("weekTrendDates",    weekDates);
        result.put("weekTrendRevenues", weekRevs);
        result.put("weekTrendCounts",   weekCnts);
        result.put("topScripts",        topScripts);
        result.put("storeId",           storeId);
        result.put("generatedAt",       LocalDateTime.now().toString());
        return result;
    }

    private BigDecimal safeQueryBig(String sql, Object... args) {
        try {
            BigDecimal r = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return r != null ? r : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("safeQueryBig: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** 安全执行 COUNT 查询，失败返回0 */
    private int safeQueryInt(String sql, Object... args) {
        try {
            Integer r = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return r != null ? r : 0;
        } catch (Exception e) {
            log.warn("safeQueryInt: {}", e.getMessage());
            return 0;
        }
    }

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
