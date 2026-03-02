package com.murder.common.exception;

/**
 * 用户不存在异常
 */
public class UserNotFoundException extends BaseException {

    public UserNotFoundException() {
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
