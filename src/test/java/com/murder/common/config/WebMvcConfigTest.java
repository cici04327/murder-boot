package com.murder.common.config;

import com.murder.interceptor.JwtTokenInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("WebMvc AI 路由放行配置测试")
class WebMvcConfigTest {

    @Test
    @DisplayName("AI客服接口保持公开，AI排班接口不在公开白名单")
    void aiPublicRoutes_ShouldRemainExcludedWhileScheduleProtected() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(config, "jwtTokenInterceptor", mock(JwtTokenInterceptor.class));

        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);

        List<?> registrations = (List<?>) ReflectionTestUtils.getField(registry, "registrations");
        assertNotNull(registrations);
        assertFalse(registrations.isEmpty());

        Object registration = registrations.get(0);
        List<String> includePatterns = getPatternList(registration, "includePatterns", "pathPatterns");
        List<String> excludePatterns = getPatternList(registration, "excludePatterns");

        assertTrue(includePatterns.contains("/api/**"));
        assertTrue(excludePatterns.contains("/api/ai/chat"));
        assertTrue(excludePatterns.contains("/api/ai/recommend/**"));
        assertTrue(excludePatterns.contains("/api/ai/log"));
        assertTrue(excludePatterns.contains("/api/ai/faq"));
        assertTrue(excludePatterns.contains("/api/ai/feedback"));
        assertFalse(excludePatterns.contains("/api/ai/schedule/**"));
    }

    @SuppressWarnings("unchecked")
    private List<String> getPatternList(Object registration, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = ReflectionTestUtils.getField(registration, fieldName);
            if (value instanceof List) {
                return (List<String>) value;
            }
        }
        fail("未找到路径配置字段");
        return List.of();
    }
}
