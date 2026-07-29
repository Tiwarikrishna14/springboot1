package com.company.orderapproval.user.dto;

import com.company.orderapproval.user.entity.UserStatus;
import com.company.orderapproval.user.entity.UserType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID organizationId,
        UUID branchId,
        UUID businessCustomerId,
        UUID businessCustomerLocationId,
        UserType userType,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserStatus status,
        boolean emailVerified,
        int failedLoginAttempts,
        Instant accountLockedUntil,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        List<String> roles,
        List<String> permissions
) {
}
