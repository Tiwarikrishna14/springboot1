package com.company.orderapproval.audit.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

public interface AuditService {
    void record(String action,
                UUID organizationId,
                UUID userId,
                String entityType,
                UUID entityId,
                String description,
                Map<String, Object> oldValues,
                Map<String, Object> newValues,
                HttpServletRequest request);
}
