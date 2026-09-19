package com.company.orderapproval.audit.service;

import com.company.orderapproval.audit.event.AuditEvent;
import com.company.orderapproval.common.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private final ApplicationEventPublisher eventPublisher;

    public AuditServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void record(String action,
                       UUID organizationId,
                       UUID userId,
                       String entityType,
                       UUID entityId,
                       String description,
                       Map<String, Object> oldValues,
                       Map<String, Object> newValues,
                       HttpServletRequest request) {
        String ipAddress = request == null ? null : IpAddressUtil.extractClientIp(request);
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        eventPublisher.publishEvent(new AuditEvent(action, organizationId, userId, entityType, entityId,
                description, oldValues, newValues, ipAddress, userAgent));
    }
}
