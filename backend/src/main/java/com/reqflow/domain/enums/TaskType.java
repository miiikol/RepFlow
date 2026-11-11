package com.reqflow.domain.enums;

public enum TaskType {
    ANALYSIS("分析"),
    DEV("开发"),
    TEST("测试"),
    DOC("文档"),
    DEPLOY("部署");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
