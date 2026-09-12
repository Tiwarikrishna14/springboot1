package com.company.orderapproval.product.dto;
import java.math.BigDecimal;

public record ProductResponse(

        Long id,

        String category,

        String customerSellCode,

        String navItemCode,

        String itemDescription,

        String uom,

        BigDecimal unitRate,

        String imagePath,

        String status
) {
}
