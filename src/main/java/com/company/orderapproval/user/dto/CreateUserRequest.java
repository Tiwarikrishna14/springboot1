package com.company.orderapproval.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;
import com.company.orderapproval.user.entity.UserType;

public record CreateUserRequest(
        UUID organizationId,

        UUID branchId,

        UUID businessCustomerId,

        UUID businessCustomerLocationId,

        UserType userType,

        @NotBlank(message = "First name is required")
        @Size(max = 120, message = "First name must be at most 120 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 120, message = "Last name must be at most 120 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(max = 40, message = "Phone must be at most 40 characters")
        String phone,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
        )
        String password,

        List<UUID> roleIds
) {
}
