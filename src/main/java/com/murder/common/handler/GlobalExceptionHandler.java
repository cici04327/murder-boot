package com.murder.common.handler;

import com.murder.common.exception.BaseException;
import com.murder.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获参数类型不匹配异常（如前端传了 "undefined" 给 Long 类型参数）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<String> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object value = ex.getValue();
        log.warn("参数类型不匹配: 参数名={}, 传入值={}, 期望类型={}", paramName, value,
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        return Result.error("参数 [" + paramName + "] 值无效: " + value);
    }

    /**
     * 捕获请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("请求方法不支持: {}", ex.getMessage());
        return Result.error("请求方法不支持: " + ex.getMethod());
    }

    /**
     * 捕获资源不存在异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("请求资源不存在: {}", ex.getMessage());
        return Result.error(404, "请求地址不存在");
    }

    /**
     * 捕获业务异常
     */
    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException ex) {
        log.error("业务异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获权限异常
     */
    @ExceptionHandler(SecurityException.class)
    public Result<String> handleSecurityException(SecurityException ex) {
        log.warn("权限异常: {}", ex.getMessage());
        return Result.error(403, ex.getMessage() != null ? ex.getMessage() : "没有权限执行该操作");
    }

    /**
     * 捕获SQL异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> handleSQLException(SQLIntegrityConstraintViolationException ex) {
        log.error("SQL异常: {}", ex.getMessage());
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String duplicateValue = split[2];
            return Result.error("数据重复: " + duplicateValue);
        } else {
            return Result.error("数据库操作失败");
        }
    }

    /**
     * 捕获数据完整性异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("数据完整性异常: {}", ex.getMessage(), ex);
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (message != null && message.contains("doesn't have a default value")) {
            return Result.error("保存数据失败，缺少必要字段");
        }
        return Result.error("数据库操作失败");
    }

    /**
     * 捕获客户端主动断开连接异常
     */
    @ExceptionHandler(ClientAbortException.class)
    public Result<String> handleClientAbort(ClientAbortException ex) {
        log.warn("客户端已断开连接: {}", ex.getMessage());
        return Result.error(499, "客户端已断开连接");
    }

    /**
     * 捕获运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException ex) {
        // 打印请求上下文，便于排查前端偶发 500
        try {
            org.springframework.web.context.request.RequestAttributes ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                var req = attrs.getRequest();
                log.error("运行时异常: uri={}, method={}, query={}, msg={}", req.getRequestURI(), req.getMethod(), req.getQueryString(), ex.getMessage(), ex);
            } else {
                log.error("运行时异常: {}", ex.getMessage(), ex);
            }
        } catch (Exception ignore) {
            log.error("运行时异常: {}", ex.getMessage(), ex);
        }
        return Result.error(ex.getMessage() != null ? ex.getMessage() : "系统错误");
    }

    /**
     * 捕获所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception ex) {
        // 打印请求上下文，便于排查前端偶发 500
        try {
            org.springframework.web.context.request.RequestAttributes ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                var req = attrs.getRequest();
                log.error("系统异常: uri={}, method={}, query={}, msg={}", req.getRequestURI(), req.getMethod(), req.getQueryString(), ex.getMessage(), ex);
            } else {
                log.error("系统异常: {}", ex.getMessage(), ex);
            }
        } catch (Exception ignore) {
            log.error("系统异常: {}", ex.getMessage(), ex);
        }
        return Result.error("系统繁忙，请稍后再试");
    }
}
