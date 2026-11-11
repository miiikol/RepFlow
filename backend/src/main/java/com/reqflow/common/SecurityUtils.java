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

    public static boolean isAdmin() {
        return currentUser().getRoles().contains("ADMIN");
    }
}
