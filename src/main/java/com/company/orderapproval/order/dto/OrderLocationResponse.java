package com.company.orderapproval.order.dto;

import java.util.UUID;

public record OrderLocationResponse(
        UUID id,
        UUID businessCustomerId,
        UUID organizationId,
        UUID branchId,
        String locationCode,
        String locationName,
        String city,
        String state,
        String address,
        String permanentAddress,
        String correspondingAddress,
        boolean sameAsPermanentAddress,
        String pincode
) {
}
