package com.reqflow.domain.enums;

public enum ReviewResult {
    PASS("通过"),
    NEED_CHANGE("需修改"),
    OBJECT("反对");

    private final String label;

    ReviewResult(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
