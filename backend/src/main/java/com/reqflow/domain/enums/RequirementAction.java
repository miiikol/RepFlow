package com.reqflow.domain.enums;

public enum RequirementAction {

    SUBMIT("提交"),
    ACCEPT("受理"),
    SUBMIT_REVIEW("提交评审"),
    REVIEW_PASS("评审通过"),
    REVIEW_REJECT("评审驳回"),
    RESUBMIT("重新提交"),
    SCHEDULE("排期"),
    START_DEV("开始开发"),
    SUBMIT_TEST("提交测试"),
    SUBMIT_ACCEPTANCE("提交验收"),
    ACCEPT_PASS("验收通过"),
    ACCEPT_REJECT("验收不通过"),
    ARCHIVE("归档"),
    HOLD("挂起"),
    RESUME("恢复"),
    CANCEL("撤销");

    private final String label;

    RequirementAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
