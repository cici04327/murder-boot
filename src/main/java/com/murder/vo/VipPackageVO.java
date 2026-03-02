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
import java.util.List;

/**
 * VIP套餐VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "VIP套餐信息")
public class VipPackageVO implements Serializable {

    @Schema(description = "套餐ID")
    private Long id;
    
    @Schema(description = "套餐名称")
    private String name;
    
    @Schema(description = "会员等级")
    private Integer level;
    
    @Schema(description = "等级名称")
    private String levelName;
    
    @Schema(description = "时长类型")
    private Integer durationType;
    
    @Schema(description = "时长天数")
    private Integer durationDays;
    
    @Schema(description = "时长描述")
    private String durationText;
    
    @Schema(description = "原价")
    private BigDecimal originalPrice;
    
    @Schema(description = "现价")
    private BigDecimal currentPrice;
    
    @Schema(description = "折扣率")
    private BigDecimal discountRate;
    
    @Schema(description = "积分倍率")
    private BigDecimal pointMultiplier;
    
    @Schema(description = "每月赠送优惠券数量")
    private Integer couponCount;
    
    @Schema(description = "预约优先")
    private Boolean priorityBooking;
    
    @Schema(description = "专属客服")
    private Boolean exclusiveService;
    
    @Schema(description = "生日特权")
    private Boolean birthdayGift;
    
    @Schema(description = "专属徽章")
    private Boolean exclusiveBadge;
    
    @Schema(description = "专属折扣")
    private BigDecimal specialDiscount;
    
    @Schema(description = "套餐描述")
    private String description;
    
    @Schema(description = "权益列表")
    private List<String> features;
    
    @Schema(description = "标签")
    private String tag;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

