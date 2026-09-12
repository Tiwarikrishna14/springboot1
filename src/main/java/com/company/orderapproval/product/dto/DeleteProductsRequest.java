package com.company.orderapproval.product.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DeleteProductsRequest(
        @NotEmpty(message = "Product ids are mandatory")
        List<@NotNull(message = "Product id cannot be null") Long> ids
) {
}
