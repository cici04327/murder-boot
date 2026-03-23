package com.murder.controller;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.common.result.Result;
import com.murder.vo.StatisticsChartsVO;
import com.murder.vo.StatisticsOverviewVO;
import com.murder.vo.StatisticsRankingVO;
import com.murder.vo.StatisticsRealtimeVO;
import com.murder.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 统计数据控制器?
 */
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "统计数据接口")
@Slf4j
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取统计概览数据
     */
    @GetMapping("/overview")
    @Operation(summary = "获取统计概览数据")
    public Result<StatisticsOverviewVO> getOverview() {
        log.info("获取统计概览数据");
        StatisticsOverviewVO overview = statisticsService.getOverview();
        return Result.success(overview);
    }

    /**
     * 获取图表数据
     */
    @GetMapping("/charts")
    @Operation(summary = "获取图表数据")
    public Result<StatisticsChartsVO> getCharts(@RequestParam(defaultValue = "7") Integer days) {
        log.info("获取图表数据: days={}", days);
        StatisticsChartsVO charts = statisticsService.getCharts(days);
        return Result.success(charts);
    }

    /**
     * 获取排行榜数据
     */
    @GetMapping("/rankings")
    @Operation(summary = "获取排行榜数据")
    public Result<StatisticsRankingVO> getRankings(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取排行榜数据: limit={}", limit);
        StatisticsRankingVO rankings = statisticsService.getRankings(limit);
        return Result.success(rankings);
    }

    /**
     * 获取实时数据
     */
    @GetMapping("/realtime")
    @Operation(summary = "获取实时数据")
    public Result<StatisticsRealtimeVO> getRealtime(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取实时数据: limit={}", limit);
        StatisticsRealtimeVO realtime = statisticsService.getRealtime(limit);
        return Result.success(realtime);
    }

    /**
     * 获取经营看板数据（转化率、房间利用率、复购率等核心经营指标）
     */
    @GetMapping("/operation-board")
    @Operation(summary = "经营看板", description = "返回转化率、房间利用率、复购率、取消率等核心经营指标")
    public Result<java.util.Map<String, Object>> getOperationBoard(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(required = false) Long storeId) {
        Long scopedStoreId = resolveAdminScopedStoreId(storeId);
        log.info("获取经营看板数据: days={}, storeId={}, scopedStoreId={}, role={}", days, storeId, scopedStoreId, BaseContext.getRole());
        return Result.success(statisticsService.getOperationBoard(days, scopedStoreId));
    }

    /**
     * 门店营收日报（门店端专用）
     * 返回今日/昨日/本周/本月营收、预约数、客单价、小时分布、剧本热度TOP5
     */
    @GetMapping("/store-daily-report")
    @Operation(summary = "门店营收日报", description = "门店端专用，返回今日/昨日/本周/本月多维度营收数据")
    public Result<java.util.Map<String, Object>> getStoreDailyReport(
            @RequestParam(required = false) Long storeId) {
        Long scopedStoreId = resolveAdminScopedStoreId(storeId);
        log.info("获取门店营收日报: storeId={}, scopedStoreId={}, role={}", storeId, scopedStoreId, BaseContext.getRole());
        return Result.success(statisticsService.getStoreDailyReport(scopedStoreId));
    }

    private Long resolveAdminScopedStoreId(Long requestedStoreId) {
        String role = BaseContext.getRole();
        if ("SUPER_ADMIN".equals(role)) {
            return requestedStoreId;
        }
        if ("STORE_ADMIN".equals(role)) {
            return BaseContext.getStoreId();
        }
        if ("STORE_STAFF".equals(role) && hasPermission("report:view")) {
            Long storeId = BaseContext.getStoreId();
            if (storeId == null) {
                throw new BaseException("当前员工账号未绑定门店");
            }
            return storeId;
        }
        throw new BaseException("无权限访问经营统计数据");
    }

    private boolean hasPermission(String permissionCode) {
        String permissionCodes = BaseContext.getPermissionCodes();
        if (permissionCodes == null || permissionCode == null) {
            return false;
        }
        for (String code : permissionCodes.split(",")) {
            if (permissionCode.equals(code != null ? code.trim() : null)) {
                return true;
            }
        }
        return false;
    }
}
