package com.company.orderapproval.policy.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.policy.dto.OrderApprovalPolicyConfig;
import com.company.orderapproval.policy.service.ApprovalPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-customers/{customerId}/approval-policy")
public class ApprovalPolicyController {
    private final ApprovalPolicyService service;
    public ApprovalPolicyController(ApprovalPolicyService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<OrderApprovalPolicyConfig>> get(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.success("Approval policy fetched successfully",
                service.getOrderPolicy(customerId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    public ResponseEntity<ApiResponse<OrderApprovalPolicyConfig>> upsert(
            @PathVariable UUID customerId,
            @Valid @RequestBody OrderApprovalPolicyConfig request) {
        return ResponseEntity.ok(ApiResponse.success("Approval policy saved successfully",
                service.upsertOrderPolicy(customerId, request)));
    }
}

