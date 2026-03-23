package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 门店员工DTO
 */
@Data
@Schema(description = "门店员工信息")
public class StoreEmployeeDTO implements Serializable {

    @Schema(description = "员工ID")
    private Long id;
    
    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "员工姓名")
    private String name;
    
    @Schema(description = "联系电话")
    private String phone;
    
    @Schema(description = "职位：1店长，2副店长，3主持人，4服务员")
    private Integer position;
    
    @Schema(description = "入职日期")
    private LocalDate joinDate;
    
    @Schema(description = "月薪")
    private BigDecimal salary;
    
    @Schema(description = "状态：1在职，0离职")
    private Integer status;

    @Schema(description = "登录账号")
    private String loginAccount;

    @Schema(description = "登录密码，留空则沿用原密码；新增时留空则默认123456")
    private String loginPassword;

    @Schema(description = "员工后台角色：MANAGER / CLERK / DM")
    private String staffRole;

    @Schema(description = "权限编码，逗号分隔")
    private String permissionCodes;

    @Schema(description = "绑定DM ID")
    private Long dmId;
}
