package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 剧本评价DTO
 */
@Data
@Schema(description = "剧本评价请求")
public class ScriptReviewDTO implements Serializable {

    @Schema(description = "剧本ID")
    private Long scriptId;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "评分")
    private BigDecimal rating;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "图片")
    private String images;
}

