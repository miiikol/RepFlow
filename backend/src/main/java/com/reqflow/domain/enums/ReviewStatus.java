package com.reqflow.domain.enums;

public enum ReviewStatus {
    PENDING("待评审"),
    PASSED("已通过"),
    REJECTED("已驳回"),
    CANCELLED("已取消");

    private final String label;

    ReviewStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
