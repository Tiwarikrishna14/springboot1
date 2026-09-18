package com.company.orderapproval.branch.controller;

import com.company.orderapproval.branch.dto.*;
import com.company.orderapproval.branch.service.BranchService;
import com.company.orderapproval.common.response.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRANCH_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> list(@RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return ResponseEntity.ok(ApiResponse
            .success("Branches fetched successfully", 
            PageResponse.from(service.list(organizationId, p))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_VIEW')")
    public ResponseEntity<ApiResponse<BranchResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Branch fetched successfully", service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BRANCH_CREATE')")
    public ResponseEntity<ApiResponse<BranchResponse>> create(@RequestParam UUID organizationId, @Valid @RequestBody CreateBranchRequest r, HttpServletRequest h) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Branch created successfully", service.create(organizationId, r, h)));
    }

    @PutMapping("/{organizationId}")
    @PreAuthorize("hasAuthority('BRANCH_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String branchCode,
            @Valid @RequestBody UpdateBranchRequest r,
            HttpServletRequest h) {
        service.update(branchId, organizationId, branchCode, r, h);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully"));
    }

    @GetMapping("/{id}/delete-validation")
    @PreAuthorize("hasAuthority('BRANCH_UPDATE')")
    public ResponseEntity<ApiResponse<DeleteValidationResponse>> validateDelete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Branch delete validation fetched successfully", service.validateDelete(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_UPDATE')")
    public ResponseEntity<ApiResponse<BranchResponse>> delete(@PathVariable UUID id,
                                                              @RequestParam(defaultValue = "false") boolean force,
                                                              HttpServletRequest h) {
        return ResponseEntity.ok(ApiResponse.success("Branch deactivated successfully", service.delete(id, force, h)));
    }
}
