package com.company.orderapproval.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SupplierActionRequest(
        @NotNull(message = "Supplier action is required")
        SupplierAction action,

        LocalDate expectedDeliveryDate,

        @Size(max = 1000, message = "Remark must be at most 1000 characters")
        String remark
) {
}
