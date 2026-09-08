package com.company.orderapproval.branch.dto;
import com.company.orderapproval.branch.entity.BranchStatus;
import java.time.Instant; import java.util.UUID;
public record BranchResponse(UUID id, UUID organizationId, String branchCode, String name, String city, String address,
                             BranchStatus status, Instant createdAt, Instant updatedAt) {}
