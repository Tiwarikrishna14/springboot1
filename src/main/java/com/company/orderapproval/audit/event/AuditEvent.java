package com.company.orderapproval.audit.event;

import java.util.Map;
import java.util.UUID;

/** Event emitted when an auditable business action occurs. */
public record AuditEvent(
        String action,
        UUID organizationId,
        UUID userId,
        String entityType,
        UUID entityId,
        String description,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String ipAddress,
        String userAgent
) {
}
