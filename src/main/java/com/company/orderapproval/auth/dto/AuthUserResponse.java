package com.company.orderapproval.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UUID organizationId,
        List<String> roles,
        List<String> permissions
) {
}
