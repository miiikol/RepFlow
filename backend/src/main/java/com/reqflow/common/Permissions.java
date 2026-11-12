package com.reqflow.common;

/**
 * 权限点编码。
 * <p>
 * 状态机、数据权限、服务层越权判断统一使用权限点，不再硬编码角色码；
 * 角色只是权限点的集合，调整角色授权不需要改代码。
 */
public final class Permissions {

    /** 系统管理员特权：拥有该权限点即视为管理员 */
    public static final String ADMIN = "sys:admin";

    public static final String REQUIREMENT_SUBMIT = "requirement:submit";
    public static final String REQUIREMENT_ACCEPT = "requirement:accept";
    public static final String REQUIREMENT_SUBMIT_REVIEW = "requirement:submit-review";
    public static final String REQUIREMENT_REVIEW_DECIDE = "requirement:review-decide";
    public static final String REQUIREMENT_EDIT_ANY = "requirement:edit-any";
    public static final String REQUIREMENT_SCHEDULE = "requirement:schedule";
    public static final String REQUIREMENT_DEVELOP = "requirement:develop";
    public static final String REQUIREMENT_SUBMIT_TEST = "requirement:submit-test";
    public static final String REQUIREMENT_SUBMIT_ACCEPTANCE = "requirement:submit-acceptance";
    public static final String REQUIREMENT_ACCEPT_DECIDE = "requirement:accept-decide";
    public static final String REQUIREMENT_ARCHIVE = "requirement:archive";
    public static final String REQUIREMENT_HOLD = "requirement:hold";
    public static final String REQUIREMENT_CANCEL = "requirement:cancel";
    public static final String CHANGE_APPROVE = "change:approve";

    private Permissions() {
    }
}
