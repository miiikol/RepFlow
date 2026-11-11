package com.reqflow.domain.enums;

public enum ChangeStatus {
    PENDING("待审批"),
    APPROVED("已批准"),
    REJECTED("已驳回"),
    APPLIED("已生效");

    private final String label;

    ChangeStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
