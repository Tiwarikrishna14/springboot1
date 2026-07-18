package com.company.orderapproval.organization.dto;

import com.company.orderapproval.organization.entity.OrganizationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationStatusRequest(
        @NotNull(message = "Status is required")
        OrganizationStatus status
) {
}
