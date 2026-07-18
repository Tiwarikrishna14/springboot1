package com.company.orderapproval.organization.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.dto.OrganizationResponse;
import com.company.orderapproval.organization.dto.UpdateOrganizationRequest;
import com.company.orderapproval.organization.dto.UpdateOrganizationStatusRequest;
import com.company.orderapproval.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(summary = "List organizations")
    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Organizations fetched successfully",
                PageResponse.from(organizationService.list(pageable))
        ));
    }

    @Operation(summary = "Get organization by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Organization fetched successfully", organizationService.get(id)));
    }

    @Operation(summary = "Create organization")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('ORGANIZATION_CREATE')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(@Valid @RequestBody CreateOrganizationRequest request,
                                                                    HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created successfully", organizationService.create(request, servletRequest)));
    }

    @Operation(summary = "Update organization")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN') and hasAuthority('ORGANIZATION_UPDATE')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateOrganizationRequest request,
                                                                    HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Organization updated successfully",
                organizationService.update(id, request, servletRequest)
        ));
    }

    @Operation(summary = "Update organization status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('ORGANIZATION_UPDATE')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateStatus(@PathVariable UUID id,
                                                                          @Valid @RequestBody UpdateOrganizationStatusRequest request,
                                                                          HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Organization status updated successfully",
                organizationService.updateStatus(id, request, servletRequest)
        ));
    }
}
