package com.murder.interceptor;

import com.murder.common.properties.JwtProperties;
import com.murder.controller.AiScheduleController;
import com.murder.service.AiScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("JWT拦截器 AI 排班鉴权测试")
class JwtTokenInterceptorTest {

    private JwtTokenInterceptor interceptor;
    private AiScheduleController aiScheduleController;

    @BeforeEach
    void setUp() {
        interceptor = new JwtTokenInterceptor();
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setTokenName("token");
        jwtProperties.setUserSecretKey("user-test-secret");
        jwtProperties.setAdminSecretKey("admin-test-secret");
        ReflectionTestUtils.setField(interceptor, "jwtProperties", jwtProperties);

        aiScheduleController = new AiScheduleController(mock(AiScheduleService.class));
    }

    @Test
    @DisplayName("AI排班接口未登录时应被拦截")
    void aiScheduleEndpoint_RequiresAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/schedule/task/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Method method = AiScheduleController.class.getMethod("listTasks", Long.class, int.class);
        HandlerMethod handlerMethod = new HandlerMethod(aiScheduleController, method);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("登录已过期"));
    }
}
