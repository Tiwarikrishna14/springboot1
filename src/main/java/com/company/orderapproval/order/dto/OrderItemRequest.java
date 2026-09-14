package com.company.orderapproval.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
        @Digits(integer = 14, fraction = 3, message = "Quantity must have maximum 14 integer digits and 3 decimal places")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price must be greater than or equal to 0")
        @Digits(integer = 15, fraction = 2, message = "Unit price must have maximum 15 integer digits and 2 decimal places")
        BigDecimal unitPrice,

        @Size(max = 500, message = "Line remark must be at most 500 characters")
        String remark
) {
}
