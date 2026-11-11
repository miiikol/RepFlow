package com.reqflow.domain.enums;

public enum RequirementStatus {

    DRAFT("草稿"),
    PENDING_ACCEPT("待受理"),
    ANALYZING("分析中"),
    PENDING_REVIEW("待评审"),
    REVIEW_PASSED("评审通过"),
    SCHEDULED("已排期"),
    DEVELOPING("开发中"),
    TESTING("测试中"),
    PENDING_ACCEPTANCE("待验收"),
    ACCEPTED("已验收"),
    ARCHIVED("已归档"),
    REJECTED("已驳回"),
    CANCELLED("已撤销"),
    ON_HOLD("已挂起");

    private final String label;

    RequirementStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static String labelOf(String code) {
        for (RequirementStatus s : values()) {
            if (s.name().equals(code)) {
                return s.label;
            }
        }
        return code;
    }
}
