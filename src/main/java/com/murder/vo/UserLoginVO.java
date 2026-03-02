package com.murder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 用户登录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class UserLoginVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "会员等级")
    private Integer memberLevel;
    
    @Schema(description = "积分")
    private Integer points;
    
    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "角色：SUPER_ADMIN/STORE_ADMIN/USER")
    private String role;

    @Schema(description = "门店ID（门店管理员有值）")
    private Long storeId;
}
