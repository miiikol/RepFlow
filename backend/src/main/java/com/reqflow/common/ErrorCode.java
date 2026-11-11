package com.reqflow.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNAUTHORIZED(10001, "未登录或登录已过期"),
    ACCOUNT_LOCKED(10002, "账号已锁定，请 15 分钟后再试"),
    BAD_CREDENTIALS(10003, "用户名或密码错误"),
    ACCOUNT_DISABLED(10004, "账号已停用"),
    FORBIDDEN(20001, "无操作权限"),
    DATA_FORBIDDEN(20002, "无权访问该数据"),
    VALIDATION_ERROR(30001, "参数校验失败"),
    BIZ_ERROR(40001, "业务处理失败"),
    ILLEGAL_TRANSITION(40002, "当前状态不允许该操作"),
    NOT_FOUND(40004, "数据不存在"),
    READONLY(40005, "数据已归档，只读"),
    CONFLICT(40007, "数据已被他人修改，请刷新后重试"),
    INTERNAL_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
