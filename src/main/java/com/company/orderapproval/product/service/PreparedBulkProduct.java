package com.company.orderapproval.product.service;

import java.math.BigDecimal;

record PreparedBulkProduct(String category, String navItemCode, String itemDescription,
                           String uom, BigDecimal unitRate, String imagePath, int rowNumber) {}

