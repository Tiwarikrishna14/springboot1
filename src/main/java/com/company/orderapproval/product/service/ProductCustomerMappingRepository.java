package com.company.orderapproval.product.service;

import com.company.orderapproval.product.entity.ProductCustomerMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCustomerMappingRepository extends JpaRepository<ProductCustomerMapping, Long> {
    Optional<ProductCustomerMapping> findByProductIdAndBusinessCustomerId(Long productId, UUID businessCustomerId);
    boolean existsByProductIdAndBusinessCustomerId(Long productId, UUID businessCustomerId);
    boolean existsByBusinessCustomerIdAndProductNavItemCodeIgnoreCase(UUID businessCustomerId, String navItemCode);
    @Query("select mapping from ProductCustomerMapping mapping where upper(mapping.businessCustomer.customerCode) = upper(:customerCode)")
    Page<ProductCustomerMapping> findByBusinessCustomerCustomerCodeIgnoreCase(@Param("customerCode") String customerCode, Pageable pageable);

    @Query("select count(mapping) from ProductCustomerMapping mapping where upper(mapping.businessCustomer.customerCode) in :customerCodes")
    long countByBusinessCustomerCustomerCodeIn(@Param("customerCodes") Collection<String> customerCodes);
    List<ProductCustomerMapping> findByProductIdIn(Collection<Long> productIds);

    @Modifying
    @Query("delete from ProductCustomerMapping mapping where mapping.product.id in :productIds")
    int deleteByProductIds(@Param("productIds") Collection<Long> productIds);
}
