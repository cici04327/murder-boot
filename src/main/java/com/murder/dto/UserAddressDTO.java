package com.murder.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户地址DTO
 */
@Data
public class UserAddressDTO implements Serializable {
    
    /**
     * 地址ID（编辑时需要）
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 联系人姓名
     */
    private String contactName;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 区县
     */
    private String district;
    
    /**
     * 详细地址
     */
    private String detail;
    
    /**
     * 是否默认地址：1是，0否
     */
    private Integer isDefault;
}
