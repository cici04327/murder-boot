package com.murder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户登录DTO
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginDTO implements Serializable {

    @Schema(description = "用户名或手机号")
    private String username;
    
    @Schema(description = "密码")
    private String password;
}
