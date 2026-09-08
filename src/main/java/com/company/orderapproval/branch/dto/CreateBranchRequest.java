package com.company.orderapproval.branch.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateBranchRequest(@NotBlank @Size(max=64) String branchCode, @NotBlank @Size(max=255) String name,
                                  @Size(max=120) String city, @Size(max=500) String address) {}
