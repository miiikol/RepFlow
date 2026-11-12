package com.reqflow.common;

import com.reqflow.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser user)) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long userId() {
        return currentUser().getId();
    }

    /** 管理员判定走权限点，不再依赖角色码 */
    public static boolean isAdmin() {
        return hasPermission(Permissions.ADMIN);
    }

    public static boolean hasPermission(String permission) {
        return hasPermission(currentUser(), permission);
    }

    public static boolean hasPermission(LoginUser user, String permission) {
        if (user == null || user.getPermissions() == null) {
            return false;
        }
        return user.getPermissions().contains(Permissions.ADMIN) || user.getPermissions().contains(permission);
    }
}
