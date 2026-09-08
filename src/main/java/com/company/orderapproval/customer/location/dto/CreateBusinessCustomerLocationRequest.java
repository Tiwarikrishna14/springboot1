package com.company.orderapproval.customer.location.dto;
import jakarta.validation.constraints.*;
public record CreateBusinessCustomerLocationRequest(@NotBlank @Size(max=64) String locationCode,@NotBlank @Size(max=255) String locationName,@NotBlank @Size(max=120) String city,@Size(max=120) String state,@Size(max=500) String address,@Size(max=20) String pincode) {}
