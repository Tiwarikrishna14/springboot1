package com.company.orderapproval.policy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderApprovalPolicyConfig(
        @NotNull ApprovalMode approvalMode,
        boolean selfApprovalAllowed,
        @NotEmpty List<@Valid ApprovalLevelPolicy> levels
) {
    public static OrderApprovalPolicyConfig defaultPolicy() {
        return new OrderApprovalPolicyConfig(
                ApprovalMode.PARALLEL,
                true,
                List.of(new ApprovalLevelPolicy(1, 1, ApprovalCompletionRule.ALL, null))
        );
    }
}
