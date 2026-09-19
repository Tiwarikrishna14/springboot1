package com.company.orderapproval.product.service;

import com.company.orderapproval.product.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByNavItemCodeIgnoreCase(String navItemCode);

}
