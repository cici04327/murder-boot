package com.murder.controller;

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
}
