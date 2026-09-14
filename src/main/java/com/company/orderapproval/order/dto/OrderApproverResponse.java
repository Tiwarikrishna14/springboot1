package com.company.orderapproval.order.dto;

import com.company.orderapproval.order.entity.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderApproverResponse(
        UUID id,
        UUID userId,
        ApprovalStatus approvalStatus,
        String remark,
        Instant actedAt
) {
}
