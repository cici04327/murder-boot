package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评价信息")
public class ReviewVO implements Serializable {

    @Schema(description = "评价ID")
    private Long id;
    
    @Schema(description = "预约ID")
    private Long reservationId;
    
    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "门店名称")
    private String storeName;
    
    @Schema(description = "剧本ID")
    private Long scriptId;
    
    @Schema(description = "剧本名称")
    private String scriptName;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户名")
    private String userName;
    
    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "是否匿名：1是，0否")
    private Integer isAnonymous;

    @Schema(description = "综合评分")
    private Integer overallRating;

    @Schema(description = "门店评分")
    private Integer storeRating;

    @Schema(description = "剧本评分")
    private Integer scriptRating;

    @Schema(description = "服务评分")
    private Integer serviceRating;

    @Schema(description = "评分")
    private BigDecimal rating;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "图片")
    private String images;
    
    @Schema(description = "回复内容")
    private String replyContent;
    
    @Schema(description = "回复时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;
    
    @Schema(description = "是否精选")
    private Integer isFeatured;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
