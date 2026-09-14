package com.company.orderapproval.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_number_counters")
public class OrderNumberCounter {

    @Id
    @Column(name = "business_customer_id")
    private UUID businessCustomerId;

    @Column(name = "company_code", nullable = false, length = 3)
    private String companyCode;

    @Column(name = "next_sequence", nullable = false)
    private long nextSequence = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
