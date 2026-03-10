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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约信息")
public class ReservationVO implements Serializable {

    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "门店地址")
    private String storeAddress;

    @Schema(description = "门店电话")
    private String storePhone;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "房间名称")
    private String roomName;

    @Schema(description = "房间容量")
    private Integer roomCapacity;

    @Schema(description = "剧本ID")
    private Long scriptId;

    @Schema(description = "剧本名称")
    private String scriptName;

    @Schema(description = "剧本封面")
    private String scriptCover;

    @Schema(description = "预约人数")
    private Integer playerCount;

    @Schema(description = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reservationTime;

    @Schema(description = "预约时长")
    private BigDecimal duration;

    @Schema(description = "总价")
    private BigDecimal totalPrice;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal actualAmount;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "预约状态")
    private Integer status;

    @Schema(description = "支付状态")
    private Integer payStatus;

    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundApplyTime;

    @Schema(description = "退款处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundProcessTime;

    @Schema(description = "退款状态")
    private Integer refundStatus;

    @Schema(description = "核销码")
    private String checkInCode;

    @Schema(description = "核销状态")
    private Integer checkInStatus;

    @Schema(description = "核销时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;

    @Schema(description = "管理员备注")
    private String adminRemark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "是否已评价")
    private Integer hasReviewed;

    @Schema(description = "VIP折扣")
    private BigDecimal vipDiscount;

    @Schema(description = "VIP优惠金额")
    private BigDecimal vipDiscountAmount;

    @Schema(description = "VIP等级名称")
    private String vipLevelName;
}
