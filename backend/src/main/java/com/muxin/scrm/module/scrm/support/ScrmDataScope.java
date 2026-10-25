package com.muxin.scrm.module.scrm.support;

import com.muxin.scrm.security.util.SecurityUtils;

import java.util.Set;

public final class ScrmDataScope {

    private ScrmDataScope() {
    }

    public static boolean canViewAll() {
        Set<String> roles = SecurityUtils.getRoles();
        return SecurityUtils.isRoot()
                || roles.contains("ADMIN")
                || roles.contains("DEPT_MANAGER");
    }

    public static Long scopeOwnerId() {
        return canViewAll() ? null : SecurityUtils.getUserId();
    }
}
