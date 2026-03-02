package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时数据")
public class StatisticsRealtimeVO implements Serializable {

    @Schema(description = "最新预约动态")
    private List<RecentReservation> recentReservations;
    
    @Schema(description = "今日热门剧本TOP5")
    private List<HotScript> hotScripts;
    
    @Schema(description = "最近优惠券使用")
    private List<RecentCouponUse> recentCouponUses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReservation implements Serializable {
        @Schema(description = "用户昵称")
        private String userNickname;
        
        @Schema(description = "剧本名称")
        private String scriptName;
        
        @Schema(description = "门店名称")
        private String storeName;
        
        @Schema(description = "预约时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reservationTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotScript implements Serializable {
        @Schema(description = "剧本名称")
        private String name;
        
        @Schema(description = "今日预约数")
        private Integer todayCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentCouponUse implements Serializable {
        @Schema(description = "用户昵称")
        private String userNickname;
        
        @Schema(description = "优惠券名称")
        private String couponName;
        
        @Schema(description = "使用时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime useTime;
    }
}
