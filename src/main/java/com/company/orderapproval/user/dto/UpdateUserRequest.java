package com.company.orderapproval.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 120, message = "First name must be at most 120 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 120, message = "Last name must be at most 120 characters")
        String lastName,

        @Email(message = "Email must be valid")
        String email,

        @Size(max = 40, message = "Phone must be at most 40 characters")
        String phone
) {
}
