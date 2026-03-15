package com.murder.interceptor;

import com.murder.common.context.BaseContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ThreadLocal 清理过滤器
 * 确保每次请求结束后清理 BaseContext 中的线程本地变量，
 * 防止线程池复用时上一个请求的用户信息泄漏到下一个请求。
 */
@Component
@Slf4j
public class ContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // 无论请求是否成功，都清理 ThreadLocal，防止内存泄漏
            BaseContext.removeCurrentId();
            log.debug("=== ContextCleanupFilter: ThreadLocal cleared after request");
        }
    }
}
