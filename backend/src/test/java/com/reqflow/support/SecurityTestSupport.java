package com.reqflow.support;

import com.reqflow.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** 测试用登录上下文构造器，避免每个测试类重复拼装 LoginUser */
public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static LoginUser user(long id, String username, Long deptId, String dataScope, String... roles) {
        LoginUser user = new LoginUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setDeptId(deptId);
        user.setStatus(1);
        user.setRoles(List.copyOf(Arrays.asList(roles)));
        user.setPermissions(Set.of());
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
