package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 门店VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店信息")
public class StoreVO implements Serializable {

    @Schema(description = "门店ID")
    private Long id;
    
    @Schema(description = "门店名称")
    private String name;
    
    @Schema(description = "门店地址")
    private String address;
    
    @Schema(description = "联系电话")
    private String phone;
    
    @Schema(description = "门店图片")
    private String images;
    
    @Schema(description = "门店简介")
    private String description;
    
    @Schema(description = "营业开始时间")
    private String openTime;
    
    @Schema(description = "营业结束时间")
    private String closeTime;
    
    @Schema(description = "经度")
    private BigDecimal longitude;
    
    @Schema(description = "纬度")
    private BigDecimal latitude;
    
    @Schema(description = "评分")
    private BigDecimal rating;
    
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "门店登录账号")
    private String loginAccount;
    
    @Schema(description = "距离（公里）")
    private Double distance;
}
