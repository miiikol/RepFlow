package com.reqflow.domain.enums;

public enum TaskStatus {
    TODO("待开始"),
    IN_PROGRESS("进行中"),
    DONE("已完成"),
    BLOCKED("已阻塞"),
    CANCELLED("已取消");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
