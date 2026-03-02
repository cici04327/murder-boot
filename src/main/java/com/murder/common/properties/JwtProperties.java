package com.murder.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Component
@ConfigurationProperties(prefix = "murder.jwt")
@Data
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secretKey;
    
    /**
     * JWT过期时间（毫秒）
     */
    private long ttl;
    
    /**
     * JWT令牌名称
     */
    private String tokenName;
    
    /**
     * 用户端JWT密钥
     */
    private String userSecretKey;
    
    /**
     * 用户端JWT过期时间（毫秒）
     */
    private long userTtl;
    
    /**
     * 用户端JWT令牌名称
     */
    private String userTokenName;
    
    /**
     * 管理端JWT密钥
     */
    private String adminSecretKey;
    
    /**
     * 管理端JWT过期时间（毫秒）
     */
    private long adminTtl;
    
    /**
     * 管理端JWT令牌名称
     */
    private String adminTokenName;
}
