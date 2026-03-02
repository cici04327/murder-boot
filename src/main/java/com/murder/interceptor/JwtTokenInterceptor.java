package com.murder.interceptor;

import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.context.BaseContext;
import com.murder.common.properties.JwtProperties;
import com.murder.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.info("=== JWT Interceptor - URI: {}, Method: {}, Handler: {}", uri, method, handler.getClass().getSimpleName());
        
        if (!(handler instanceof HandlerMethod)) {
            log.info("=== Not a HandlerMethod, allowing access");
            return true;
        }

        String token = request.getHeader(jwtProperties.getTokenName());
        
        log.info("=== JWT interceptor executing for URI: {}, token exists: {}", uri, token != null);

        try {
            // 智能选择密钥：通过请求头 X-Client-Type 判断
            String clientType = request.getHeader("X-Client-Type");
            String secretKey;
            
            // 优先使用请求头标识
            if ("admin".equals(clientType)) {
                secretKey = jwtProperties.getAdminSecretKey();
                log.info("=== Using admin secret key (from header) for URI: {}", uri);
            } else if ("user".equals(clientType)) {
                secretKey = jwtProperties.getUserSecretKey();
                log.info("=== Using user secret key (from header) for URI: {}", uri);
            } else if (uri.startsWith("/api/admin") || uri.contains("/admin/")) {
                // 兜底：根据URI判断
                secretKey = jwtProperties.getAdminSecretKey();
                log.info("=== Using admin secret key (from URI) for URI: {}", uri);
            } else {
                // 默认使用用户密钥
                secretKey = jwtProperties.getUserSecretKey();
                log.info("=== Using user secret key (default) for URI: {}", uri);
            }
            
            Claims claims = JwtUtil.parseJWT(secretKey, token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            String role = claims.get(JwtClaimsConstant.ROLE) != null ? String.valueOf(claims.get(JwtClaimsConstant.ROLE)) : null;
            Long storeId = claims.get(JwtClaimsConstant.STORE_ID) != null ? Long.valueOf(String.valueOf(claims.get(JwtClaimsConstant.STORE_ID))) : null;

            log.info("=== JWT validation success, userId={}, role={}, storeId={}", userId, role, storeId);

            BaseContext.setCurrentId(userId);
            BaseContext.setRole(role);
            BaseContext.setStoreId(storeId);

            // 管理端接口权限控制：必须是总部/门店管理员
            if ((uri.startsWith("/api/admin") || uri.contains("/admin/"))
                    && !("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role))) {
                log.warn("=== Admin API access denied: userId={}, role={}, uri={}", userId, role, uri);
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"没有管理端访问权限\"}");
                return false;
            }

            return true;
        } catch (Exception ex) {
            log.error("=== JWT token validation failed for URI: {}, error: {}", uri, ex.getMessage());
            
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":0,\"msg\":\"登录已过期，请重新登录\"}");
            } catch (Exception e) {
                log.error("Failed to write error response", e);
            }
            return false;
        }
    }
}
