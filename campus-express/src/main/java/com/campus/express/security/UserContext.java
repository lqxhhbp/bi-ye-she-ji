package com.campus.express.security;

import com.campus.express.common.exception.BusinessException;

/**
 * 当前请求的登录用户上下文
 */
public final class UserContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthUser get() {
        return CURRENT_USER.get();
    }

    public static AuthUser require() {
        AuthUser user = CURRENT_USER.get();
        if (user == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return user;
    }

    public static Long requireUserId() {
        return require().userId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
