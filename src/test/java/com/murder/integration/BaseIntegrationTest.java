package com.murder.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.integration.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 测试用的JWT Token（对应测试用户ID=1）
     */
    protected String testUserToken;

    /**
     * 测试用的管理员JWT Token（对应管理员ID=2）
     */
    protected String adminToken;

    @BeforeEach
    void baseSetUp() {
        // 生成测试用的JWT Token
        // 这里使用简化的方式，实际可以调用登录接口获取
        testUserToken = "Bearer test-user-token";
        adminToken = "Bearer test-admin-token";
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
