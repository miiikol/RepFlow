package com.reqflow.domain.enums;

public enum AcceptanceStatus {
    PENDING("待验收"),
    PASSED("已通过"),
    FAILED("未通过"),
    CANCELLED("已取消");

    private final String label;

    AcceptanceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
