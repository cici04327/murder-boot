package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店评价VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店评价信息")
public class StoreReviewVO implements Serializable {

    @Schema(description = "评价ID")
    private Long id;
    
    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "门店名称")
    private String storeName;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户昵称")
    private String userNickname;
    
    @Schema(description = "预约ID")
    private Long reservationId;
    
    @Schema(description = "总评分：1-5星")
    private Integer rating;
    
    @Schema(description = "环境评分")
    private Integer environmentRating;
    
    @Schema(description = "服务评分")
    private Integer serviceRating;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "商家回复")
    private String reply;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
