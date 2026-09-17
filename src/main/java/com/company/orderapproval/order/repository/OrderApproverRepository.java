package com.company.orderapproval.order.repository;

import com.company.orderapproval.order.entity.OrderApprover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderApproverRepository extends JpaRepository<OrderApprover, UUID> {
    long countByUserId(UUID userId);
}
