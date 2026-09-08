package com.company.orderapproval.product.service;

import com.company.orderapproval.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCustomerSellCodeAndNavItemCode( String customerSellCode, String navItemCode );
}
