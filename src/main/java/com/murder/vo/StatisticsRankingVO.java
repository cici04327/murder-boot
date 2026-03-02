package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 统计排行榜VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计排行榜数据")
public class StatisticsRankingVO implements Serializable {

    @Schema(description = "门店排行")
    private List<StoreRankItem> storeRankings;
    
    @Schema(description = "剧本排行")
    private List<ScriptRankItem> scriptRankings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreRankItem implements Serializable {
        @Schema(description = "门店ID")
        private Long id;
        
        @Schema(description = "门店名称")
        private String name;
        
        @Schema(description = "营业额")
        private BigDecimal revenue;
        
        @Schema(description = "预约数")
        private Integer reservationCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptRankItem implements Serializable {
        @Schema(description = "剧本ID")
        private Long id;
        
        @Schema(description = "剧本名称")
        private String name;
        
        @Schema(description = "预约次数")
        private Integer bookingCount;
        
        @Schema(description = "评分")
        private BigDecimal rating;
    }
}
