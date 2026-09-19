package com.company.orderapproval.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderApproverRequest(
        @NotNull UUID userId,
        @Min(1) int approvalLevel
) {}
