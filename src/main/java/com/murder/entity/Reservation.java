package com.murder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reservation_order")
public class Reservation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long storeId;

    private Long roomId;

    private Long scriptId;

    private Long groupId;

    private Integer playerCount;

    private LocalDateTime reservationTime;

    private BigDecimal duration;

    private BigDecimal totalPrice;

    private Long couponId;

    private BigDecimal discountAmount;

    private BigDecimal vipDiscountAmount;

    private BigDecimal vipDiscount;

    private BigDecimal actualAmount;

    private String contactName;

    private String contactPhone;

    private String remark;

    private Integer status;

    private Integer payStatus;

    private LocalDateTime payTime;

    private String refundReason;

    private LocalDateTime refundApplyTime;

    private LocalDateTime refundProcessTime;

    private Integer refundStatus;

    private String checkInCode;

    private Integer checkInStatus;

    private LocalDateTime checkInTime;

    private String adminRemark;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
