package com.company.orderapproval.permission.dto;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String name,
        String description,
        String module
) {
}
