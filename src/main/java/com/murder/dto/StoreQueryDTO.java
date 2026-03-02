package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 门店查询DTO
 */
@Data
@Schema(description = "门店查询条件")
public class StoreQueryDTO implements Serializable {
    
    @Schema(description = "门店名称")
    private String name;
    
    @Schema(description = "地址关键字")
    private String address;
    
    @Schema(description = "状态：1营业，0停业")
    private Integer status;
    
    @Schema(description = "最低评分")
    private BigDecimal minRating;
    
    @Schema(description = "最高评分")
    private BigDecimal maxRating;
    
    @Schema(description = "排序方式：distance-距离最近，rating-评分最高，hot-最热门")
    private String sortBy;
    
    @Schema(description = "用户经度（用于距离排序）")
    private BigDecimal longitude;
    
    @Schema(description = "用户纬度（用于距离排序）")
    private BigDecimal latitude;
    
    @Schema(description = "页码")
    private Integer page = 1;
    
    @Schema(description = "每页数量")
    private Integer pageSize = 10;
}
