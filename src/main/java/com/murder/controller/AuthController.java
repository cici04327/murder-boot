package com.murder.controller;

import com.murder.common.result.Result;
import com.murder.common.utils.JwtUtil;
import com.murder.common.properties.JwtProperties;
import com.murder.common.constant.JwtClaimsConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口")
@Slf4j
public class AuthController {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 验证Token
     */
    @PostMapping("/verify")
    @Operation(summary = "验证Token")
    public Result<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String token) {
        log.info("验证Token: {}", token);
        
        try {
            // 去掉Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证Token
            Map<String, Object> claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            
            return Result.success(claims);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return Result.error("Token验证失败");
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<String> refreshToken(@RequestHeader("Authorization") String token) {
        log.info("刷新Token: {}", token);
        
        try {
            // 去掉Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证Token
            Map<String, Object> claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            
            // 生成新Token
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put(JwtClaimsConstant.USER_ID, claims.get(JwtClaimsConstant.USER_ID));
            newClaims.put(JwtClaimsConstant.USERNAME, claims.get(JwtClaimsConstant.USERNAME));
            newClaims.put(JwtClaimsConstant.PHONE, claims.get(JwtClaimsConstant.PHONE));
            
            String newToken = JwtUtil.createJWT(
                    jwtProperties.getUserSecretKey(),
                    jwtProperties.getUserTtl(),
                    newClaims
            );
            
            return Result.success(newToken);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return Result.error("Token刷新失败");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> health() {
        return Result.success("认证服务运行正常");
    }
}