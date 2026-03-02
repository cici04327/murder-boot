package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 统计概览VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计概览数据")
public class StatisticsOverviewVO implements Serializable {

    @Schema(description = "今日营业额")
    private BigDecimal todayRevenue;
    
    @Schema(description = "营业额增长率(%)")
    private BigDecimal revenueGrowth;
    
    @Schema(description = "今日预约数")
    private Integer todayReservations;
    
    @Schema(description = "预约增长率(%)")
    private BigDecimal reservationGrowth;
    
    @Schema(description = "今日新增用户")
    private Integer todayNewUsers;
    
    @Schema(description = "累计用户数")
    private Integer totalUsers;
    
    @Schema(description = "在线门店数")
    private Integer onlineStores;
    
    @Schema(description = "总门店数")
    private Integer totalStores;
    
    @Schema(description = "今日优惠券使用数")
    private Integer todayCouponUsed;
    
    @Schema(description = "优惠券使用率(%)")
    private BigDecimal couponUsageRate;
    
    @Schema(description = "昨日营业额")
    private BigDecimal yesterdayRevenue;
    
    @Schema(description = "昨日预约数")
    private Integer yesterdayReservations;
    
    @Schema(description = "精选剧本数")
    private Integer totalScripts;
    
    @Schema(description = "用户满意度(%)")
    private BigDecimal satisfactionRate;
}
