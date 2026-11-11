package com.reqflow.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(ErrorCode.BIZ_ERROR.getCode(), message);
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode.getCode(), errorCode.getMessage());
    }

    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode.getCode(), message);
    }
}
