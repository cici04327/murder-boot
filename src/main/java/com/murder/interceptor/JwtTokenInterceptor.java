package com.murder.interceptor;

import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.context.BaseContext;
import com.murder.common.properties.JwtProperties;
import com.murder.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    private static final String RESERVATION_ADMIN_ACTION_PATTERN =
            "^/api/reservation/\\d+/(confirm|check-in|complete|pay|assign-dm)$";
    private static final String RESERVATION_CHECKIN_ACTION_PATTERN =
            "^/api/reservation/\\d+/check-in$";
    private static final String RESERVATION_COMPLETE_ACTION_PATTERN =
            "^/api/reservation/\\d+/complete$";
    private static final String RESERVATION_ASSIGN_DM_ACTION_PATTERN =
            "^/api/reservation/\\d+/assign-dm$";
    private static final String REFUND_PROCESS_URI = "/api/reservation/payment/refund/process";
    private static final String OPERATION_BOARD_URI = "/api/statistics/operation-board";
    private static final String STORE_DAILY_REPORT_URI = "/api/statistics/store-daily-report";
    private static final String STORE_EMPLOYEE_URI_PREFIX = "/api/store/employee";
    private static final String ADMIN_NOTIFICATION_URI_PREFIX = "/api/admin/notification";
    private static final String ADMIN_NOTIFICATION_SEND_URI = "/api/admin/notification/send";
    private static final String ARTICLE_DETAIL_PATTERN = "^/api/article/\\d+$";
    private static final String ARTICLE_COMMENT_PATTERN = "^/api/article/\\d+/comments$";
    private static final String ARTICLE_FAVORITE_STATUS_PATTERN = "^/api/article/\\d+/favorite/status$";

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
        boolean adminProtectedRequest = isAdminProtectedRequest(uri, method);
        boolean optionalAuthRequest = isOptionalAuthRequest(uri, method);
        
        log.info("=== JWT interceptor executing for URI: {}, token exists: {}, optionalAuth: {}",
                uri, token != null, optionalAuthRequest);

        if (!StringUtils.hasText(token)) {
            if (optionalAuthRequest) {
                log.info("=== Optional auth request without token, allowing anonymous access: {}", uri);
                return true;
            }
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"登录已过期，请重新登录\"}");
            return false;
        }

        try {
            // 智能选择密钥：通过请求头 X-Client-Type 判断
            String clientType = request.getHeader("X-Client-Type");
            String secretKey;
            boolean ignoreExpiration = false;
            
            // 优先使用请求头标识
            if (adminProtectedRequest) {
                secretKey = jwtProperties.getAdminSecretKey();
                ignoreExpiration = true;
                log.info("=== Using admin secret key (protected action) for URI: {}", uri);
            } else if ("admin".equals(clientType)) {
                secretKey = jwtProperties.getAdminSecretKey();
                ignoreExpiration = true;
                log.info("=== Using admin secret key (from header) for URI: {}", uri);
            } else if ("user".equals(clientType)) {
                secretKey = jwtProperties.getUserSecretKey();
                log.info("=== Using user secret key (from header) for URI: {}", uri);
            } else if (uri.startsWith("/api/admin") || uri.contains("/admin/")) {
                // 兜底：根据URI判断
                secretKey = jwtProperties.getAdminSecretKey();
                ignoreExpiration = true;
                log.info("=== Using admin secret key (from URI) for URI: {}", uri);
            } else {
                // 默认使用用户密钥
                secretKey = jwtProperties.getUserSecretKey();
                log.info("=== Using user secret key (default) for URI: {}", uri);
            }
            
            Claims claims = ignoreExpiration
                    ? JwtUtil.parseJWTIgnoreExpiration(secretKey, token)
                    : JwtUtil.parseJWT(secretKey, token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            String role = claims.get(JwtClaimsConstant.ROLE) != null ? String.valueOf(claims.get(JwtClaimsConstant.ROLE)) : null;
            Long storeId = claims.get(JwtClaimsConstant.STORE_ID) != null ? Long.valueOf(String.valueOf(claims.get(JwtClaimsConstant.STORE_ID))) : null;
            Long employeeId = claims.get(JwtClaimsConstant.EMPLOYEE_ID) != null
                    ? Long.valueOf(String.valueOf(claims.get(JwtClaimsConstant.EMPLOYEE_ID)))
                    : null;
            String staffRole = claims.get(JwtClaimsConstant.STAFF_ROLE) != null
                    ? String.valueOf(claims.get(JwtClaimsConstant.STAFF_ROLE))
                    : null;
            String permissionCodes = claims.get(JwtClaimsConstant.PERMISSION_CODES) != null
                    ? String.valueOf(claims.get(JwtClaimsConstant.PERMISSION_CODES))
                    : null;
            Long dmId = claims.get(JwtClaimsConstant.DM_ID) != null
                    ? Long.valueOf(String.valueOf(claims.get(JwtClaimsConstant.DM_ID)))
                    : null;

            log.info("=== JWT validation success, userId={}, role={}, storeId={}, employeeId={}, staffRole={}",
                    userId, role, storeId, employeeId, staffRole);

            BaseContext.setCurrentId(userId);
            BaseContext.setRole(role);
            BaseContext.setStoreId(storeId);
            BaseContext.setEmployeeId(employeeId);
            BaseContext.setStaffRole(staffRole);
            BaseContext.setPermissionCodes(permissionCodes);
            BaseContext.setDmId(dmId);

            // 管理端接口权限控制：必须是总部/门店管理员
            if (adminProtectedRequest
                    && !isAllowedAdminRoleForUri(role, uri, staffRole)) {
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

    private boolean isAdminProtectedRequest(String uri, String method) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/api/admin") || uri.contains("/admin/")) {
            return true;
        }
        if (uri.startsWith(STORE_EMPLOYEE_URI_PREFIX) && !"/api/store/employee/login".equals(uri)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && REFUND_PROCESS_URI.equals(uri)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)
                && (OPERATION_BOARD_URI.equals(uri) || STORE_DAILY_REPORT_URI.equals(uri))) {
            return true;
        }
        return "PUT".equalsIgnoreCase(method) && uri.matches(RESERVATION_ADMIN_ACTION_PATTERN);
    }

    private boolean isAllowedAdminRoleForUri(String role, String uri, String staffRole) {
        if ("SUPER_ADMIN".equals(role) || "STORE_ADMIN".equals(role)) {
            return true;
        }
        if (!"STORE_STAFF".equals(role) || uri == null) {
            return false;
        }
        if (uri.startsWith(STORE_EMPLOYEE_URI_PREFIX)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), "employee:manage");
        }
        if (ADMIN_NOTIFICATION_SEND_URI.equals(uri)) {
            return false;
        }
        if (uri.startsWith(ADMIN_NOTIFICATION_URI_PREFIX)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), "notification:view");
        }
        if (OPERATION_BOARD_URI.equals(uri) || STORE_DAILY_REPORT_URI.equals(uri)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), "report:view");
        }
        if (uri.matches(RESERVATION_CHECKIN_ACTION_PATTERN)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), "reservation:checkin");
        }
        if (uri.matches(RESERVATION_COMPLETE_ACTION_PATTERN)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), "reservation:complete");
        }
        if (REFUND_PROCESS_URI.equals(uri) || uri.matches(RESERVATION_ASSIGN_DM_ACTION_PATTERN)) {
            return hasPermissionCode(BaseContext.getPermissionCodes(), REFUND_PROCESS_URI.equals(uri) ? "refund:process" : "reservation:assign_dm");
        }
        return false;
    }

    private boolean hasPermissionCode(String permissionCodes, String permissionCode) {
        if (!StringUtils.hasText(permissionCodes) || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        for (String code : permissionCodes.split(",")) {
            if (permissionCode.equals(code != null ? code.trim() : null)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOptionalAuthRequest(String uri, String method) {
        if (uri == null || method == null) {
            return false;
        }
        // POST请求中允许匿名的接口
        if ("POST".equalsIgnoreCase(method)) {
            return uri.equals("/api/ai/chat")
                    || uri.equals("/api/ai/service")
                    || uri.equals("/api/store/page/advanced")
                    || uri.startsWith("/api/script/page")
                    || uri.equals("/api/script/list")
                    || uri.startsWith("/api/group/page")
                    || uri.startsWith("/api/search");
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        // 文章相关
        if (uri.matches(ARTICLE_DETAIL_PATTERN)
                || uri.matches(ARTICLE_COMMENT_PATTERN)
                || uri.matches(ARTICLE_FAVORITE_STATUS_PATTERN)) {
            return true;
        }
        // 剧本相关（列表、详情、分类、评价、收藏状态、角色）
        if (uri.startsWith("/api/script")) {
            return true;
        }
        // 门店相关（列表、详情、房间、评价）
        if (uri.startsWith("/api/store") && !uri.startsWith("/api/store/employee")) {
            return true;
        }
        // 排期相关
        if (uri.startsWith("/api/schedule") || uri.startsWith("/api/script-schedule")) {
            return true;
        }
        // 拼单相关（列表、详情、热门）
        if (uri.startsWith("/api/group")) {
            return true;
        }
        // 搜索
        if (uri.startsWith("/api/search")) {
            return true;
        }
        // 文章列表
        if (uri.startsWith("/api/article")) {
            return true;
        }
        // DM列表（预约页展示用）
        if (uri.startsWith("/api/dm") && "GET".equalsIgnoreCase(method)) {
            return true;
        }
        // 首页推荐、统计等公开接口
        if (uri.startsWith("/api/recommend") || uri.startsWith("/api/home")) {
            return true;
        }
        return false;
    }
}
