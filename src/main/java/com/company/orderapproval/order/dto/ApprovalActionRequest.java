package com.company.orderapproval.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalActionRequest(
        @NotNull(message = "Approval action is required")
        ApprovalAction action,

        @Size(max = 1000, message = "Remark must be at most 1000 characters")
        String remark
) {
}
