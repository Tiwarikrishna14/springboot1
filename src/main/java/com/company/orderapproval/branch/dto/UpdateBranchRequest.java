package com.company.orderapproval.branch.dto;
import com.company.orderapproval.branch.entity.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record UpdateBranchRequest(@NotBlank @Size(max=255) String name, @Size(max=120) String city,
                                  @Size(max=500) String address, @NotNull BranchStatus status) {}
