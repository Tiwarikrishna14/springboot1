package com.company.orderapproval.branch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @Entity @Table(name = "branches")
public class Branch {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_code", nullable = false, length = 64) private String branchCode;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 120) private String city;
    @Column(length = 500) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private BranchStatus status = BranchStatus.ACTIVE;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @PrePersist void prePersist() { Instant now = Instant.now(); createdAt = now; updatedAt = now; normalize(); }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); normalize(); }
    private void normalize() { if (branchCode != null) branchCode = branchCode.trim().toUpperCase(); }
}
