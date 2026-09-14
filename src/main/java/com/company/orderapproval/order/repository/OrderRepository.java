package com.company.orderapproval.order.repository;

import com.company.orderapproval.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findDetailedByIdForUpdate(@Param("id") UUID id);
}
