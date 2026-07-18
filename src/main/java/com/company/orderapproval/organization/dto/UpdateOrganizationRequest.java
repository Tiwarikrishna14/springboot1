package com.company.orderapproval.organization.dto;

import com.company.orderapproval.organization.entity.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 255, message = "Organization name must be at most 255 characters")
        String name,

        @NotNull(message = "Organization type is required")
        OrganizationType organizationType,

        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be at most 320 characters")
        String email,

        @Size(max = 40, message = "Phone must be at most 40 characters")
        String phone
) {
}
