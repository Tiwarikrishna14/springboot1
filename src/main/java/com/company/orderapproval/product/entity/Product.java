package com.company.orderapproval.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(name = "status")
    private String status;

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
}
