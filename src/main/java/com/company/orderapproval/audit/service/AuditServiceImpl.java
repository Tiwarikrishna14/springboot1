package com.company.orderapproval.audit.service;

import com.company.orderapproval.audit.entity.AuditLog;
import com.company.orderapproval.audit.repository.AuditLogRepository;
import com.company.orderapproval.common.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setOrganizationId(organizationId);
            auditLog.setUserId(userId);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setOldValues(oldValues);
            auditLog.setNewValues(newValues);
            if (request != null) {
                auditLog.setIpAddress(IpAddressUtil.extractClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            auditLogRepository.save(auditLog);
        } catch (RuntimeException ex) {
            log.warn("Audit log write failed for action {}", action, ex);
        }
    }
}
