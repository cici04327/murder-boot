package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 评价统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评价统计信息")
public class ReviewStatisticsVO implements Serializable {

    @Schema(description = "总评价数")
    private Long totalCount;
    
    @Schema(description = "平均评分")
    private BigDecimal averageRating;
    
    @Schema(description = "5星数量")
    private Long fiveStarCount;
    
    @Schema(description = "4星数量")
    private Long fourStarCount;
    
    @Schema(description = "3星数量")
    private Long threeStarCount;
    
    @Schema(description = "2星数量")
    private Long twoStarCount;
    
    @Schema(description = "1星数量")
    private Long oneStarCount;
    
    @Schema(description = "好评数量")
    private Integer goodReviews;
    
    @Schema(description = "中评数量")
    private Integer mediumReviews;
    
    @Schema(description = "差评数量")
    private Integer badReviews;
    
    @Schema(description = "好评率")
    private BigDecimal goodRate;
}
