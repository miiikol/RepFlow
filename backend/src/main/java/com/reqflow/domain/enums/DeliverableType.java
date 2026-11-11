package com.reqflow.domain.enums;

import java.util.List;

public enum DeliverableType {
    SPEC("需求规格说明书"),
    REVIEW_RECORD("评审记录"),
    TEST_REPORT("测试报告"),
    DEPLOY_DOC("部署说明"),
    ACCEPTANCE("验收单"),
    CLOSURE("结项报告"),
    CHANGE_RECORD("变更记录"),
    OTHER("其他");

    private final String label;

    DeliverableType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 发起验收时要求已登记交付物的类型 */
    public static List<String> requiredTypes() {
        return List.of(SPEC.name(), TEST_REPORT.name(), DEPLOY_DOC.name());
    }
}
