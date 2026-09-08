package com.company.orderapproval.customer.entity;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.UuidGenerator; import java.time.Instant; import java.util.UUID;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="business_customers") public class BusinessCustomer {
 @Id @GeneratedValue @UuidGenerator private UUID id; @Column(name="organization_id",nullable=false) private UUID organizationId; @Column(name="branch_id",nullable=false) private UUID branchId;
 @Column(name="customer_code",nullable=false,length=64) private String customerCode; @Column(nullable=false,length=255) private String name; @Column(length=320) private String email; @Column(length=40) private String phone;
 @Column(length=120) private String city; @Column(length=120) private String state; @Column(length=500) private String address; @Column(length=20) private String pincode;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private BusinessCustomerStatus status=BusinessCustomerStatus.ACTIVE; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; @Column(name="created_by") private UUID createdBy; @Column(name="updated_by") private UUID updatedBy;
 @PrePersist void prePersist(){Instant n=Instant.now();createdAt=n;updatedAt=n;normalize();} @PreUpdate void preUpdate(){updatedAt=Instant.now();normalize();} private void normalize(){if(customerCode!=null)customerCode=customerCode.trim().toUpperCase();}
}
