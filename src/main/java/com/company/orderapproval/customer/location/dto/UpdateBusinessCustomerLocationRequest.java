package com.company.orderapproval.customer.location.dto;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus; import jakarta.validation.constraints.*;
public record UpdateBusinessCustomerLocationRequest(@NotBlank @Size(max=255) String locationName,@NotBlank @Size(max=120) String city,@Size(max=120) String state,@Size(max=500) String address,@Size(max=20) String pincode,@NotNull BusinessCustomerLocationStatus status) {}
