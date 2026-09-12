package com.company.orderapproval.product.service;

record BulkProductRow(
        String category,
        String navItemCode,
        String itemDescription,
        String uom,
        String unitRateValue,
        String imageFilename,
        int rowNumber
) {
}
