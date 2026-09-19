package com.company.orderapproval.dashboard.model;

import com.company.orderapproval.auth.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public record DashboardContext(
        UUID userId,
        UUID organizationId,
        UUID branchId,
        UUID businessCustomerId,
        List<String> roles
) {
    public static DashboardContext from(AuthenticatedUser user) {
        return new DashboardContext(user.userId(), user.organizationId(), user.branchId(),
                user.businessCustomerId(), user.roles());
    }

    public boolean hasRole(String role) { return roles.contains(role); }
}

