package com.murder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券DTO
 */
@Data
@Schema(description = "优惠券请求")
public class CouponDTO implements Serializable {

    @Schema(description = "优惠券ID")
    private Long id;
    
    @Schema(description = "优惠券名称", required = true)
    private String name;
    
    @Schema(description = "类型：1满减券，2折扣券，3代金券", required = true)
    private Integer type;
    
    @Schema(description = "折扣值：满减券为减免金额，折扣券为折扣比例（如0.8表示8折）", required = true)
    private BigDecimal discountValue;
    
    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;
    
    @Schema(description = "发行总量", required = true)
    private Integer totalCount;
    
    @Schema(description = "有效期开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStartTime;
    
    @Schema(description = "有效期结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEndTime;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "兑换所需积分（0表示免费领取）")
    private Integer exchangePoints;
    
    @Schema(description = "状态：1上架，0下架")
    private Integer status;
}
