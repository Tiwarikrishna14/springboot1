package com.company.orderapproval.order.event;

import java.util.UUID;

public record OrderEvent(
        String action,
        UUID orderId,
        String orderNumber,
        UUID businessCustomerId,
        UUID actorUserId
) {
}
