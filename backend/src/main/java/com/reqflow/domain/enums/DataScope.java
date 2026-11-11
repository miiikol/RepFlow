package com.reqflow.domain.enums;

public enum DataScope {
    ALL("全部"),
    DEPT("本部门"),
    PROJECT("本项目"),
    SELF("本人");

    private final String label;

    DataScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
