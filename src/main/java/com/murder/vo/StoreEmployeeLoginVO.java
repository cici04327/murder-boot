package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店员工登录响应")
public class StoreEmployeeLoginVO implements Serializable {

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "登录账号")
    private String loginAccount;

    @Schema(description = "员工后台角色：STORE_STAFF")
    private String role;

    @Schema(description = "员工岗位角色：MANAGER / CLERK / DM")
    private String staffRole;

    @Schema(description = "权限编码")
    private String permissionCodes;

    @Schema(description = "绑定DM ID")
    private Long dmId;

    @Schema(description = "JWT令牌")
    private String token;
}
