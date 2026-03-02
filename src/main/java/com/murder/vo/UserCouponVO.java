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
 * 用户优惠券VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户优惠券信息")
public class UserCouponVO implements Serializable {

    @Schema(description = "用户优惠券ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "优惠券ID")
    private Long couponId;
    
    @Schema(description = "优惠券名称")
    private String couponName;
    
    @Schema(description = "类型：1满减券，2折扣券，3代金券")
    private Integer type;
    
    @Schema(description = "类型名称")
    private String typeName;
    
    @Schema(description = "折扣值")
    private BigDecimal discountValue;
    
    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;
    
    @Schema(description = "订单ID")
    private Long orderId;
    
    @Schema(description = "订单编号")
    private String orderNo;
    
    @Schema(description = "状态：1未使用，2已使用，3已过期")
    private Integer status;
    
    @Schema(description = "状态名称")
    private String statusName;
    
    @Schema(description = "领取时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    
    @Schema(description = "使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime useTime;
    
    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    @Schema(description = "是否可用")
    private Boolean canUse;
}
