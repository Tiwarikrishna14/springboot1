package com.company.orderapproval.customer.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferBusinessCustomerBranchRequest(
        @NotNull UUID targetBranchId
) {
}
