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
 * 优惠券VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券信息")
public class CouponVO implements Serializable {

    @Schema(description = "优惠券ID")
    private Long id;
    
    @Schema(description = "优惠券名称")
    private String name;
    
    @Schema(description = "类型：1满减券，2折扣券，3代金券")
    private Integer type;
    
    @Schema(description = "类型名称")
    private String typeName;
    
    @Schema(description = "折扣值")
    private BigDecimal discountValue;
    
    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;
    
    @Schema(description = "发行总量")
    private Integer totalCount;
    
    @Schema(description = "剩余数量")
    private Integer remainCount;
    
    @Schema(description = "已领取数量")
    private Integer receivedCount;
    
    @Schema(description = "已使用数量")
    private Integer usedCount;
    
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
    
    @Schema(description = "状态名称")
    private String statusName;
    
    @Schema(description = "是否可领取")
    private Boolean canReceive;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
