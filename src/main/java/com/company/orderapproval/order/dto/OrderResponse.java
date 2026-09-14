package com.company.orderapproval.order.dto;

import com.company.orderapproval.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID organizationId,
        UUID branchId,
        UUID businessCustomerId,
        UUID businessCustomerLocationId,
        String businessCustomerCode,
        String businessCustomerName,
        UUID createdBy,
        String notes,
        String remarks,
        String priority,
        String location,
        String referenceNumber,
        OrderStatus status,
        LocalDate expectedDeliveryDate,
        BigDecimal totalAmount,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> products,
        List<OrderApproverResponse> approvers
) {
}
