package com.company.orderapproval.role.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignPermissionsRequest(
        @NotEmpty(message = "At least one permission is required")
        List<UUID> permissionIds
) {
}
