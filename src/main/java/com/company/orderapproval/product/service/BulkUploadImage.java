package com.company.orderapproval.product.service;

public record BulkUploadImage(String fileName, String contentType, byte[] bytes) {
}
