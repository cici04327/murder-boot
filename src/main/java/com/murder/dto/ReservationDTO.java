package com.murder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约DTO
 */
@Data
@Schema(description = "预约请求")
public class ReservationDTO implements Serializable {

    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "房间ID")
    private Long roomId;
    
    @Schema(description = "剧本ID")
    private Long scriptId;
    
    @Schema(description = "预约人数")
    private Integer playerCount;
    
    @Schema(description = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime reservationTime;
    
    @Schema(description = "预约时长（小时）")
    private BigDecimal duration;
    
    @Schema(description = "总价格")
    private BigDecimal totalPrice;
    
    @Schema(description = "联系人")
    private String contactName;
    
    @Schema(description = "联系电话")
    private String contactPhone;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户优惠券ID")
    private Long userCouponId;

    @Schema(description = "前端预计算的VIP折扣金额")
    private BigDecimal vipDiscountAmount;

    @Schema(description = "前端预计算的VIP折扣率")
    private BigDecimal vipDiscount;

    @Schema(description = "前端预计算的实付金额")
    private BigDecimal actualAmount;
}
