package com.company.orderapproval.branch.dto;
import com.company.orderapproval.branch.entity.BranchStatus;
import jakarta.validation.constraints.Size;
public record UpdateBranchRequest(
        @Size(max = 255) String name,
        @Size(max = 120) String city,
        @Size(max = 500) String address,
        BranchStatus status
) {}
