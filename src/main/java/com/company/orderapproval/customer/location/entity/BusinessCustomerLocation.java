package com.company.orderapproval.customer.location.entity;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.UuidGenerator; import java.time.Instant; import java.util.UUID;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="business_customer_locations") public class BusinessCustomerLocation {
 @Id @GeneratedValue @UuidGenerator private UUID id; @Column(name="business_customer_id",nullable=false) private UUID businessCustomerId; @Column(name="organization_id",nullable=false) private UUID organizationId; @Column(name="branch_id",nullable=false) private UUID branchId;
 @Column(name="location_code",nullable=false,length=64) private String locationCode; @Column(name="location_name",nullable=false,length=255) private String locationName; @Column(nullable=false,length=120) private String city; @Column(length=120) private String state; @Column(length=500) private String address; @Column(length=20) private String pincode;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private BusinessCustomerLocationStatus status=BusinessCustomerLocationStatus.ACTIVE; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist void prePersist(){Instant n=Instant.now();createdAt=n;updatedAt=n;normalize();} @PreUpdate void preUpdate(){updatedAt=Instant.now();normalize();} private void normalize(){if(locationCode!=null)locationCode=locationCode.trim().toUpperCase();}
}
