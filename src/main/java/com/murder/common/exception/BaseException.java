package com.murder.common.exception;

/**
 * 业务异常基类
 */
public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String message) {
        super(message);
    }
}
