package com.reqflow.support;

import com.reqflow.common.Permissions;
import com.reqflow.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 测试用登录上下文构造器，避免每个测试类重复拼装 LoginUser */
public final class SecurityTestSupport {

    /** 与 V4 迁移脚本中的角色授权保持一致，便于单测复现生产权限 */
    private static final Map<String, Set<String>> PERMISSIONS_BY_ROLE = Map.of(
            "ADMIN", Set.of(Permissions.ADMIN),
            "BUSINESS", Set.of(Permissions.REQUIREMENT_SUBMIT, Permissions.REQUIREMENT_CANCEL),
            "RA", Set.of(Permissions.REQUIREMENT_SUBMIT, Permissions.REQUIREMENT_ACCEPT,
                    Permissions.REQUIREMENT_SUBMIT_REVIEW, Permissions.REQUIREMENT_REVIEW_DECIDE,
                    Permissions.REQUIREMENT_EDIT_ANY, Permissions.REQUIREMENT_SUBMIT_ACCEPTANCE,
                    Permissions.REQUIREMENT_ARCHIVE, Permissions.REQUIREMENT_HOLD, Permissions.REQUIREMENT_CANCEL),
            "PM", Set.of(Permissions.REQUIREMENT_SCHEDULE, Permissions.REQUIREMENT_DEVELOP,
                    Permissions.REQUIREMENT_SUBMIT_TEST, Permissions.REQUIREMENT_SUBMIT_ACCEPTANCE,
                    Permissions.REQUIREMENT_ACCEPT_DECIDE, Permissions.REQUIREMENT_ARCHIVE,
                    Permissions.REQUIREMENT_HOLD, Permissions.REQUIREMENT_CANCEL),
            "DEV", Set.of(Permissions.REQUIREMENT_DEVELOP, Permissions.REQUIREMENT_SUBMIT_TEST),
            "QA", Set.of(Permissions.REQUIREMENT_SUBMIT_TEST, Permissions.REQUIREMENT_SUBMIT_ACCEPTANCE),
            "ACCEPT", Set.of(Permissions.REQUIREMENT_ACCEPT_DECIDE));

    private SecurityTestSupport() {
    }

    /** 按角色推导权限点，让不加载数据库的单元测试也能走权限点校验 */
    public static Set<String> permissionsOf(String... roles) {
        Set<String> permissions = new LinkedHashSet<>();
        for (String role : roles) {
            permissions.addAll(PERMISSIONS_BY_ROLE.getOrDefault(role, Set.of()));
        }
        return permissions;
    }

    public static LoginUser user(long id, String username, Long deptId, String dataScope, String... roles) {
        LoginUser user = new LoginUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setDeptId(deptId);
        user.setStatus(1);
        user.setRoles(List.copyOf(Arrays.asList(roles)));
        user.setPermissions(permissionsOf(roles));
        user.setDataScope(dataScope);
        return user;
    }

    public static void authenticate(LoginUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
