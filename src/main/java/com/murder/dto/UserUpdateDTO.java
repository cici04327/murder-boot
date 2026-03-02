package com.murder.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO implements Serializable {
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 性别：1男，2女
     */
    private Integer gender;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 所在城市
     */
    private String city;
}
