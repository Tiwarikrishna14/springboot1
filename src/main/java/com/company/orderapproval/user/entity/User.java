package com.company.orderapproval.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "business_customer_id")
    private UUID businessCustomerId;

    @Column(name = "business_customer_location_id")
    private UUID businessCustomerLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 32)
    private UserType userType;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public boolean isLockedNow() {
        return status == UserStatus.LOCKED
                || (accountLockedUntil != null && accountLockedUntil.isAfter(Instant.now()));
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        normalizeEmail();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalizeEmail();
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
