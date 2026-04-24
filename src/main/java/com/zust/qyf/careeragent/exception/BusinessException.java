package com.zust.qyf.careeragent.exception;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用的业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}