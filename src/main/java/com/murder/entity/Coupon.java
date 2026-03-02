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
 * 优惠券实�?
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 优惠券名�?
     */
    private String name;
    
    /**
     * 类型�?满减券，2折扣券，3代金�?
     */
    private Integer type;
    
    /**
     * 折扣值：满减券为减免金额，折扣券为折扣比例（�?.8表示8折）
     */
    private BigDecimal discountValue;
    
    /**
     * 最低消费金�?
     */
    private BigDecimal minAmount;
    
    /**
     * 发行总量
     */
    private Integer totalCount;
    
    /**
     * 剩余数量
     */
    @TableField("received_count")
    private Integer remainCount;
    
    /**
     * 有效期开始时�?
     */
    @TableField("valid_start")
    private LocalDateTime validStartTime;
    
    /**
     * 有效期结束时�?
     */
    @TableField("valid_end")
    private LocalDateTime validEndTime;
    
    /**
     * 描述
     */
    @TableField(value = "description", exist = false)
    private String description;
    
    /**
     * 兑换所需积分�?表示免费领取�?
     */
    private Integer exchangePoints;
    
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
