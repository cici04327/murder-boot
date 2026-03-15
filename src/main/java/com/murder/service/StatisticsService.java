package com.murder.service;

import com.murder.vo.StatisticsChartsVO;
import com.murder.vo.StatisticsOverviewVO;
import com.murder.vo.StatisticsRankingVO;
import com.murder.vo.StatisticsRealtimeVO;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取统计概览数据
     */
    StatisticsOverviewVO getOverview();

    /**
     * 获取图表数据
     * @param days 天数�?�?0�?
     */
    StatisticsChartsVO getCharts(Integer days);

    /**
     * 获取排行榜数�?
     * @param limit 限制数量
     */
    StatisticsRankingVO getRankings(Integer limit);

    /**
     * 获取实时数据
     * @param limit 限制数量
     */
    StatisticsRealtimeVO getRealtime(Integer limit);

    /**
     * 获取经营看板数据（转化率、房间利用率、复购率等）
     * @param days 统计天数（默认30天）
     * @param storeId 门店ID（null=全部门店）
     */
    java.util.Map<String, Object> getOperationBoard(Integer days, Long storeId);

    /**
     * 门店营收日报（今日/昨日/本周/本月，含各时段预约分布）
     * @param storeId 门店ID（门店端必传）
     */
    java.util.Map<String, Object> getStoreDailyReport(Long storeId);
}
