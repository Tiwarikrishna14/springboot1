package com.company.orderapproval.product.service;

import com.company.orderapproval.product.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByNavItemCodeIgnoreCase(String navItemCode);

    @Query("select p from Product p where upper(p.navItemCode) in :codes")
    List<Product> findByNavItemCodes(@Param("codes") Collection<String> codes);

}
