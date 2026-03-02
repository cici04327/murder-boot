package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * VIP套餐DTO
 */
@Data
@Schema(description = "VIP套餐请求")
public class VipPackageDTO implements Serializable {

    @Schema(description = "套餐ID（更新时必填）")
    private Long id;
    
    @Schema(description = "套餐名称", required = true)
    private String name;
    
    @Schema(description = "会员等级：1普通，2银卡，3金卡，4钻石", required = true)
    private Integer level;
    
    @Schema(description = "时长类型：1月，2季，3半年，4年", required = true)
    private Integer durationType;
    
    @Schema(description = "时长天数", required = true)
    private Integer durationDays;
    
    @Schema(description = "原价", required = true)
    private BigDecimal originalPrice;
    
    @Schema(description = "现价", required = true)
    private BigDecimal currentPrice;
    
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
    
    @Schema(description = "状态：1上架，0下架")
    private Integer status;
}

