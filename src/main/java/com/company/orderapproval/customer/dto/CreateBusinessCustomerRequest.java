package com.company.orderapproval.customer.dto;
import jakarta.validation.constraints.*;
public record CreateBusinessCustomerRequest(@NotBlank @Size(max=64) String customerCode,@NotBlank @Size(max=255) String name,@Size(max=120) String city,@Size(max=120) String state,@Size(max=500) String address,@Size(max=20) String pincode,@Email @Size(max=320) String email,@Size(max=40) String phone) {}
