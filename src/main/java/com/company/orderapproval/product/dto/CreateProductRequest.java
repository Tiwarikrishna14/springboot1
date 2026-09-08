package com.company.orderapproval.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Category is mandatory")
        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category,

        @NotBlank(message = "Customer seller code is mandatory")
        @Size(max = 100, message = "Customer seller code cannot exceed 100 characters")
        String customerSellCode,

        @NotBlank(message = "NAV item code is mandatory")
        @Size(max = 100, message = "NAV item code cannot exceed 100 characters")
        String navItemCode,

        @NotBlank(message = "Item description is mandatory")
        @Size(max = 500, message = "Item description cannot exceed 500 characters")
        String itemDescription,

        @NotBlank(message = "UOM is mandatory")
        @Size(max = 20, message = "UOM cannot exceed 20 characters")
        String uom,

        @NotNull(message = "Unit rate is mandatory")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Unit rate must be greater than 0")
        @Digits(integer = 15, fraction = 2,
                message = "Unit rate must have maximum 15 integer digits and 2 decimal places")
        BigDecimal unitRate
) {
}
