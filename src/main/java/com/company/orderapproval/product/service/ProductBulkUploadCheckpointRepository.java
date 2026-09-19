package com.company.orderapproval.product.service;

import com.company.orderapproval.product.entity.ProductBulkUploadCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProductBulkUploadCheckpointRepository extends JpaRepository<ProductBulkUploadCheckpoint, UUID> {
    Optional<ProductBulkUploadCheckpoint> findByOwnerIdAndCustomerSellCodeAndFileHash(
            UUID ownerId, String customerSellCode, String fileHash);
}

