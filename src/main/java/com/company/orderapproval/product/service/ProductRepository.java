package com.company.orderapproval.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.company.orderapproval.product.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCustomerSellCodeAndNavItemCode( String customerSellCode, String navItemCode );

    boolean existsByCustomerSellCodeAndNavItemCodeAndIdNot(String customerSellCode, String navItemCode, Long id);

    Page<Product> findByCustomerSellCode(String customerCode, Pageable pageable);

}
