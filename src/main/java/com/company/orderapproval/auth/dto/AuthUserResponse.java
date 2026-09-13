package com.company.orderapproval.auth.dto;

import com.company.orderapproval.user.entity.UserType;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserType userType,
        UUID organizationId,
        String organizationName,
        UUID branchId,
        String branchName,
        UUID businessCustomerId,
        String businessCustomerName,
        List<String> roles,
        List<String> permissions
) {
}
