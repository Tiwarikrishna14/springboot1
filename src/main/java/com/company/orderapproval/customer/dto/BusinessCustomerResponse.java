package com.company.orderapproval.customer.dto;
import com.company.orderapproval.customer.entity.BusinessCustomerStatus; import java.time.Instant; import java.util.UUID;
public record BusinessCustomerResponse(UUID id,UUID organizationId,UUID branchId,String customerCode,String name,String city,String state,String address,String pincode,String email,String phone,BusinessCustomerStatus status,Instant createdAt,Instant updatedAt) {}
