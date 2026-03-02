package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 门店统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店统计信息")
public class StoreStatisticsVO implements Serializable {

    @Schema(description = "门店总数")
    private Long totalStores;
    
    @Schema(description = "营业中门店数")
    private Long openStores;
    
    @Schema(description = "停业门店数")
    private Long closedStores;
    
    @Schema(description = "总房间数")
    private Long totalRooms;
    
    @Schema(description = "可用房间数")
    private Long availableRooms;
    
    @Schema(description = "平均评分")
    private BigDecimal averageRating;
    
    @Schema(description = "评价总数")
    private Long totalReviews;
    
    @Schema(description = "好评数量")
    private Long goodReviews;
}
