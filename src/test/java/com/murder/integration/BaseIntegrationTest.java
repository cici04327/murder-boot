package com.murder.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.properties.JwtProperties;
import com.murder.common.utils.JwtUtil;
import com.murder.integration.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 集成测试基类
 * 所有集成测试类都应该继承此类
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional  // 每个测试方法执行后自动回滚
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtProperties jwtProperties;

    /**
     * 测试用的JWT Token（对应测试用户ID=1）
     */
    protected String testUserToken;

    /**
     * 测试用的管理员JWT Token（对应管理员ID=2）
     */
    protected String adminToken;

    /**
     * 测试用的管理端JWT Token（用于 /api/admin/** 接口）
     */
    protected String adminApiToken;

    @BeforeEach
    void baseSetUp() {
        // 生成普通用户 JWT token（userId=1, role=USER）
        testUserToken = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                createClaims(1L, "testuser", "USER", null)
        );

        // 生成管理员 JWT token（用于非 /api/admin/** 的管理能力测试）
        adminToken = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                createClaims(2L, "admin", "SUPER_ADMIN", null)
        );

        // 生成真正的管理端 JWT token（用于 /api/admin/** 接口）
        adminApiToken = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                createClaims(2L, "admin", "SUPER_ADMIN", null)
        );
    }

    private Map<String, Object> createClaims(Long userId, String username, String role, Long storeId) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, userId);
        claims.put(JwtClaimsConstant.USERNAME, username);
        claims.put(JwtClaimsConstant.ROLE, role);
        if (storeId != null) {
            claims.put(JwtClaimsConstant.STORE_ID, storeId);
        }
        return claims;
    }

    /**
     * 将对象转换为JSON字符串
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 从JSON字符串解析对象
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
