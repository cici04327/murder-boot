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
 * 预约实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reservation_order")
public class Reservation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 房间ID
     */
    private Long roomId;
    
    /**
     * 剧本ID
     */
    private Long scriptId;
    
    /**
     * 预约人数
     */
    private Integer playerCount;
    
    /**
     * 预约时间
     */
    private LocalDateTime reservationTime;
    
    /**
     * 预约时长（小时）
     */
    private BigDecimal duration;
    
    /**
     * 总价�?
     */
    private BigDecimal totalPrice;
    
    /**
     * 优惠券ID
     */
    private Long couponId;
    
    /**
     * 优惠金额（含VIP折扣+优惠券，合计）
     */
    private BigDecimal discountAmount;

    /**
     * VIP折扣金额
     */
    private BigDecimal vipDiscountAmount;

    /**
     * VIP折扣率（如0.90表示9折）
     */
    private BigDecimal vipDiscount;
    
    /**
     * 实际支付金额
     */
    private BigDecimal actualAmount;
    
    /**
     * 联系�?
     */
    private String contactName;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 预约状态：1待确认，2已确认，3已完成，4已取�?
     */
    private Integer status;
    
    /**
     * 支付状态：0未支付，1已支付，2退款中�?已退�?
     */
    private Integer payStatus;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 退款原�?
     */
    private String refundReason;
    
    /**
     * 退款申请时�?
     */
    private LocalDateTime refundApplyTime;
    
    /**
     * 退款处理时�?
     */
    private LocalDateTime refundProcessTime;
    
    /**
     * 退款状态：0无退款，1退款中�?退款成功，3退款失�?
     */
    private Integer refundStatus;
    
    /**
     * 管理员备�?
     */
    private String adminRemark;
    
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
