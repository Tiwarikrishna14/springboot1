package com.company.orderapproval.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes,

        @Size(max = 1000, message = "Remarks must be at most 1000 characters")
        String remarks,

        @Size(max = 32, message = "Priority must be at most 32 characters")
        String priority,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        UUID businessCustomerLocationId,

        @Size(max = 64, message = "Location code must be at most 64 characters")
        String locationCode,

        @Size(max = 120, message = "Reference number must be at most 120 characters")
        String referenceNumber,

        @Valid
        List<OrderItemRequest> products,

        List<UUID> approverIds
) {
}
