package com.company.orderapproval.order.repository;

import com.company.orderapproval.order.entity.OrderNumberCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderNumberCounterRepository extends JpaRepository<OrderNumberCounter, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO order_number_counters (business_customer_id, company_code, next_sequence, updated_at)
            VALUES (:businessCustomerId, :companyCode, 1, now())
            ON CONFLICT (business_customer_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfMissing(@Param("businessCustomerId") UUID businessCustomerId,
                        @Param("companyCode") String companyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from OrderNumberCounter counter where counter.businessCustomerId = :businessCustomerId")
    Optional<OrderNumberCounter> findByBusinessCustomerIdForUpdate(@Param("businessCustomerId") UUID businessCustomerId);
}
