package com.example.scriptkill.exception;

/**
 * 业务校验异常：用于时代不符等需要拦截并提示场务的场景
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
