package com.company.orderapproval.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRoleRequest(
        UUID organizationId,

        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        boolean systemRole
) {
}
