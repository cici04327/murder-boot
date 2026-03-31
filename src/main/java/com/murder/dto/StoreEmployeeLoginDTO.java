package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "门店员工登录DTO")
public class StoreEmployeeLoginDTO implements Serializable {

    @Schema(description = "员工登录账号")
    private String loginAccount;

    @Schema(description = "密码")
    private String password;
}
