package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计图表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计图表数据")
public class StatisticsChartsVO implements Serializable {

    @Schema(description = "营业额趋势 - 日期列表")
    private List<String> revenueDates;
    
    @Schema(description = "营业额趋势 - 金额列表")
    private List<BigDecimal> revenueAmounts;
    
    @Schema(description = "预约来源分布")
    private Map<String, Integer> reservationSource;
    
    @Schema(description = "用户增长趋势 - 日期列表")
    private List<String> userGrowthDates;
    
    @Schema(description = "用户增长趋势 - 数量列表")
    private List<Integer> userGrowthCounts;
    
    @Schema(description = "会员等级分布")
    private Map<String, Integer> memberLevelDistribution;
}
