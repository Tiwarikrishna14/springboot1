package com.company.orderapproval.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignRolesRequest(
        @NotEmpty(message = "At least one role is required")
        List<UUID> roleIds
) {
}
