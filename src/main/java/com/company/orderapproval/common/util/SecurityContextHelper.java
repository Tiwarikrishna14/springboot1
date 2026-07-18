package com.company.orderapproval.common.util;

import com.company.orderapproval.auth.security.AuthenticatedUser;
import com.company.orderapproval.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authenticatedUser;
    }

    public static UUID getCurrentUserId() {
        return currentUser().userId();
    }

    public static UUID getCurrentOrganizationId() {
        return currentUser().organizationId();
    }

    public static boolean hasPermission(String permission) {
        return currentUser().permissions().contains(permission);
    }

    public static boolean hasRole(String role) {
        return currentUser().roles().contains(role);
    }

    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }
}
