package com.company.orderapproval.audit.event;

import com.company.orderapproval.audit.entity.AuditLog;
import com.company.orderapproval.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async("eventExecutor")
    @EventListener
    @Transactional
    public void handle(AuditEvent event) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(event.action());
            auditLog.setOrganizationId(event.organizationId());
            auditLog.setUserId(event.userId());
            auditLog.setEntityType(event.entityType());
            auditLog.setEntityId(event.entityId());
            auditLog.setDescription(event.description());
            auditLog.setOldValues(event.oldValues());
            auditLog.setNewValues(event.newValues());
            auditLog.setIpAddress(event.ipAddress());
            auditLog.setUserAgent(event.userAgent());
            auditLogRepository.save(auditLog);
        } catch (RuntimeException ex) {
            log.warn("Audit log write failed for action {}", event.action(), ex);
        }
    }
}
