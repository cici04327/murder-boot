package com.murder.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
public class JwtUtil {

    /**
     * 生成JWT令牌
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
                .claims(claims)
                .signWith(key);

        if (ttlMillis > 0) {
            long expMillis = System.currentTimeMillis() + ttlMillis;
            Date exp = new Date(expMillis);
            builder.expiration(exp);
        }

        return builder.compact();
    }

    /**
     * 解析JWT令牌
     */
    public static Claims parseJWT(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 JWT 令牌并忽略过期时间，仅用于管理端长期登录场景。
     */
    public static Claims parseJWTIgnoreExpiration(String secretKey, String token) {
        try {
            return parseJWT(secretKey, token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }
}
