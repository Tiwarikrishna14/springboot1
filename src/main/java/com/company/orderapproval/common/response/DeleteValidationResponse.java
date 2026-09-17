package com.company.orderapproval.common.response;

import java.util.List;
import java.util.Map;

public record DeleteValidationResponse(
        boolean hasWarnings,
        String message,
        List<String> warnings,
        Map<String, Long> counts
) {
}
