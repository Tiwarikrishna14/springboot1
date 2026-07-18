package com.company.orderapproval.organization.dto;

import com.company.orderapproval.organization.entity.OrganizationStatus;
import com.company.orderapproval.organization.entity.OrganizationType;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String organizationCode,
        String name,
        OrganizationType organizationType,
        String email,
        String phone,
        OrganizationStatus status,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
}
