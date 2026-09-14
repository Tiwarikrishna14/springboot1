package com.company.orderapproval.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        Long productId,
        String productCode,
        String productDescription,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String remark,
        BigDecimal lineTotal
) {
}
