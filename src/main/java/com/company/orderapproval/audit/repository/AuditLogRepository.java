package com.company.orderapproval.audit.repository;

import com.company.orderapproval.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Modifying
    @Query("update AuditLog auditLog set auditLog.userId = null where auditLog.userId = :userId")
    void clearUserReferences(@Param("userId") UUID userId);
}
