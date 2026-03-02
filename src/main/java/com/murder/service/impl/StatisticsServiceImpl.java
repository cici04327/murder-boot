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
        
        // 今日营业�?
        String sql = "SELECT COALESCE(SUM(actual_amount), 0) FROM reservation_order " +
                "WHERE create_time >= ? AND status IN (1,2,3) AND is_deleted = 0";
        BigDecimal todayRevenue = jdbcTemplate.queryForObject(sql, BigDecimal.class, todayStart);
        
        // 昨日营业�?
        BigDecimal yesterdayRevenue = jdbcTemplate.queryForObject(sql, BigDecimal.class, yesterdayStart);
        
        // 今日预约�?
        sql = "SELECT COUNT(*) FROM reservation_order WHERE create_time >= ? AND is_deleted = 0";
        Integer todayReservations = jdbcTemplate.queryForObject(sql, Integer.class, todayStart);
        
        // 昨日预约�?
        Integer yesterdayReservations = jdbcTemplate.queryForObject(sql, Integer.class, yesterdayStart);
        
        // 今日新增用户
        sql = "SELECT COUNT(*) FROM user WHERE create_time >= ? AND is_deleted = 0";
        Integer todayNewUsers = jdbcTemplate.queryForObject(sql, Integer.class, todayStart);
        
        // 累计用户�?
        sql = "SELECT COUNT(*) FROM user WHERE is_deleted = 0";
        Integer totalUsers = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 在线门店�?
        sql = "SELECT COUNT(*) FROM store WHERE status = 1 AND is_deleted = 0";
        Integer onlineStores = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 总门店数
        sql = "SELECT COUNT(*) FROM store WHERE is_deleted = 0";
        Integer totalStores = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 今日优惠券使用数
        sql = "SELECT COUNT(*) FROM user_coupon WHERE use_time >= ? AND status = 2";
        Integer todayCouponUsed = jdbcTemplate.queryForObject(sql, Integer.class, todayStart);
        
        // 优惠券使用率（今日使用数/今日领取数）
        sql = "SELECT COUNT(*) FROM user_coupon WHERE receive_time >= ?";
        Integer todayCouponReceived = jdbcTemplate.queryForObject(sql, Integer.class, todayStart);
        BigDecimal couponUsageRate = BigDecimal.ZERO;
        if (todayCouponReceived > 0) {
            couponUsageRate = BigDecimal.valueOf(todayCouponUsed)
                    .divide(BigDecimal.valueOf(todayCouponReceived), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // 精选剧本数（状态为1的剧本）
        sql = "SELECT COUNT(*) FROM script WHERE status = 1 AND is_deleted = 0";
        Integer totalScripts = jdbcTemplate.queryForObject(sql, Integer.class);
        
        // 用户满意度（基于评价的平均评分，5分制转换为百分比）
        sql = "SELECT AVG(rating) FROM script_review WHERE is_deleted = 0";
        BigDecimal avgRating = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        BigDecimal satisfactionRate = BigDecimal.ZERO;
        if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
            // 将5分制转换为百分比，例如4.5分 = 90%
            satisfactionRate = avgRating.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            // 如果没有评价数据，使用门店评价计算
            sql = "SELECT AVG(rating) FROM store_review WHERE is_deleted = 0";
            BigDecimal storeAvgRating = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            if (storeAvgRating != null && storeAvgRating.compareTo(BigDecimal.ZERO) > 0) {
                satisfactionRate = storeAvgRating.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                // 默认满意度
                satisfactionRate = BigDecimal.valueOf(95);
            }
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
    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
