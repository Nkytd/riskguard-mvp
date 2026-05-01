package com.riskguard.common.exception;

import com.riskguard.common.api.ErrorCode;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public int getCode() {
        return code;
    }
}
