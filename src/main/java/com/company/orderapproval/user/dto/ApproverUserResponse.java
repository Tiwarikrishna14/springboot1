package com.company.orderapproval.user.dto;

import java.util.UUID;

public record ApproverUserResponse(
        UUID userId,
        String name,
        String email
) {
}
