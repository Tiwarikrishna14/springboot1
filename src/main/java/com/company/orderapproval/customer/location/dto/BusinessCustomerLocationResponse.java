package com.company.orderapproval.customer.location.dto;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus; import java.time.Instant; import java.util.UUID;
public record BusinessCustomerLocationResponse(UUID id,UUID businessCustomerId,UUID organizationId,UUID branchId,String locationCode,String locationName,String city,String state,String address,String pincode,BusinessCustomerLocationStatus status,Instant createdAt,Instant updatedAt) {}
