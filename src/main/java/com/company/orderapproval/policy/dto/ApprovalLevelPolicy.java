package com.company.orderapproval.policy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ApprovalLevelPolicy(
        @Min(1) int levelNumber,
        @Min(1) int minimumApprovers,
        @NotNull ApprovalCompletionRule completionRule,
        Set<String> eligibleRoles
) {}
