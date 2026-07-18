package com.company.orderapproval.role.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID organizationId,
        String name,
        String description,
        boolean systemRole,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<String> permissions
) {
}
