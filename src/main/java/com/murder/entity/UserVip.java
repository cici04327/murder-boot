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
 * 用户VIP记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_vip")
public class UserVip implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 套餐ID
     */
    private Long packageId;
    
    /**
     * 会员等级
     */
    private Integer level;
    
    /**
     * 开始时�?
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 实付金额
     */
    private BigDecimal actualPrice;
    
    /**
     * 支付方式：ALIPAY, WECHAT, POINTS
     */
    private String paymentMethod;
    
    /**
     * 订单�?
     */
    private String orderNo;
    
    /**
     * 第三方交易号
     */
    private String transactionId;
    
    /**
     * 状态：0待支付，1生效中，2已过期，3已取消
     */
    private Integer status;
    
    /**
     * 自动续费�?否，1�?
     */
    private Integer autoRenew;
    
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

