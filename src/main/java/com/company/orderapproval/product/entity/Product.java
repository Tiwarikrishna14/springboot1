package com.company.orderapproval.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "customer_sell_code", nullable = false, length = 100)
    private String customerSellCode;

    @Column(name = "nav_item_code", nullable = false, length = 100)
    private String navItemCode;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "uom", nullable = false, length = 20)
    private String uom;

    @Column(
            name = "unit_rate",
            nullable = false,
            precision = 17,
            scale = 2
    )
    private BigDecimal unitRate;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
