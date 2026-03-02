package com.murder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP套餐配置实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vip_package")
public class VipPackage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 套餐名称
     */
    private String name;
    
    /**
     * 会员等级�?普通，2银卡�?金卡�?钻石
     */
    private Integer level;
    
    /**
     * 时长类型�?月，2季，3半年�?�?
     */
    private Integer durationType;
    
    /**
     * 时长天数
     */
    private Integer durationDays;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 现价
     */
    private BigDecimal currentPrice;
    
    /**
     * 折扣�?
     */
    private BigDecimal discountRate;
    
    /**
     * 积分倍率
     */
    private BigDecimal pointMultiplier;
    
    /**
     * 每月赠送优惠券数量
     */
    private Integer couponCount;
    
    /**
     * 预约优先�?否，1�?
     */
    private Integer priorityBooking;
    
    /**
     * 专属客服�?否，1�?
     */
    private Integer exclusiveService;
    
    /**
     * 生日特权�?否，1�?
     */
    private Integer birthdayGift;
    
    /**
     * 专属徽章�?否，1�?
     */
    private Integer exclusiveBadge;
    
    /**
     * 专属折扣（如0.95表示95折）
     */
    private BigDecimal specialDiscount;
    
    /**
     * 套餐描述
     */
    private String description;
    
    /**
     * 权益列表JSON
     */
    private String features;
    
    /**
     * 标签：推荐、最划算�?
     */
    private String tag;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态：1上架�?下架
     */
    private Integer status;
    
    /**
     * 逻辑删除�?删除�?未删�?
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

