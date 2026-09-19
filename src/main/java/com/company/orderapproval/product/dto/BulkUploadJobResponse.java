package com.company.orderapproval.product.dto;

import java.time.Instant;
import java.util.UUID;

public record BulkUploadJobResponse(
        UUID jobId,
        UUID resumeToken,
        String status,
        int totalProducts,
        int processedProducts,
        int failedProducts,
        boolean resumed,
        int resumeFromProduct,
        int progressPercent,
        Long estimatedSecondsRemaining,
        String message,
        Instant startedAt,
        Instant completedAt
) {
}
