package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 评价DTO
 */
@Data
@Schema(description = "评价请求")
public class ReviewDTO implements Serializable {

    @Schema(description = "预约ID")
    private Long reservationId;
    
    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "剧本ID")
    private Long scriptId;
    
    @Schema(description = "综合评分（1-5星）")
    private Integer overallRating;
    
    @Schema(description = "门店评分（1-5星）")
    private Integer storeRating;
    
    @Schema(description = "剧本评分（1-5星）")
    private Integer scriptRating;
    
    @Schema(description = "服务评分（1-5星）")
    private Integer serviceRating;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "评价图片，逗号分隔")
    private String images;
    
    @Schema(description = "评价标签，逗号分隔")
    private String tags;
    
    @Schema(description = "是否匿名：1是，0否")
    private Integer isAnonymous;
}
