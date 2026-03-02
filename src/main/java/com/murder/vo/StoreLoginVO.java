package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 门店管理员登录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "门店管理员登录响应")
public class StoreLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID")
    private Long storeId;
    
    @Schema(description = "门店名称")
    private String storeName;
    
    @Schema(description = "门店登录账号")
    private String loginAccount;
    
    @Schema(description = "门店地址")
    private String address;
    
    @Schema(description = "门店电话")
    private String phone;
    
    @Schema(description = "门店图片")
    private String images;
    
    @Schema(description = "JWT令牌")
    private String token;
    
    @Schema(description = "角色：STORE_ADMIN")
    private String role;
}
