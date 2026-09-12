package com.company.orderapproval.product.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

interface ProductExcelReader {
    List<BulkProductRow> readRows(MultipartFile file);
}
