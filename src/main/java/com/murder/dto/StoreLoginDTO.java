package com.murder.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 门店管理员登录DTO
 */
@Data
public class StoreLoginDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 门店登录账号
     */
    private String loginAccount;
    
    /**
     * 门店登录密码
     */
    private String password;
}
