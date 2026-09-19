package com.company.orderapproval.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "product_bulk_upload_checkpoints")
public class ProductBulkUploadCheckpoint {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(name="owner_id", nullable=false) private UUID ownerId;
    @Column(name="customer_sell_code", nullable=false, length=100) private String customerSellCode;
    @Column(name="file_hash", nullable=false, length=64) private String fileHash;
    @Column(name="total_rows", nullable=false) private int totalRows;
    @Column(name="processed_rows", nullable=false) private int processedRows;
    @Column(name="failed_row_number") private Integer failedRowNumber;
    @Column(nullable=false, length=20) private String status;
    @Column(length=1000) private String message;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @PrePersist void create(){ Instant now=Instant.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void update(){ updatedAt=Instant.now(); }
}

