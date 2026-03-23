package com.murder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店员工VO（包含门店名称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店员工信息")
public class StoreEmployeeVO implements Serializable {

    @Schema(description = "员工ID")
    private Long id;
    
    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "门店名称")
    private String storeName;
    
    @Schema(description = "员工姓名")
    private String name;
    
    @Schema(description = "联系电话")
    private String phone;
    
    @Schema(description = "职位：1店长，2副店长，3主持人，4服务员")
    private Integer position;
    
    @Schema(description = "职位名称")
    private String positionName;
    
    @Schema(description = "入职日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;
    
    @Schema(description = "月薪")
    private BigDecimal salary;
    
    @Schema(description = "状态：1在职，0离职")
    private Integer status;

    @Schema(description = "登录账号")
    private String loginAccount;

    @Schema(description = "员工后台角色：MANAGER / CLERK / DM")
    private String staffRole;

    @Schema(description = "权限编码，逗号分隔")
    private String permissionCodes;

    @Schema(description = "绑定DM ID")
    private Long dmId;

    @Schema(description = "绑定DM姓名")
    private String dmName;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
