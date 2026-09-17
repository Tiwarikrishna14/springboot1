package com.company.orderapproval.customer.location.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.customer.location.dto.BusinessCustomerLocationResponse;
import com.company.orderapproval.customer.location.dto.CreateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.dto.UpdateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus;
import com.company.orderapproval.customer.location.service.BusinessCustomerLocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-customer-locations")
public class BusinessCustomerLocationController {

    private final BusinessCustomerLocationService service;

    public BusinessCustomerLocationController(BusinessCustomerLocationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<BusinessCustomerLocationResponse>>> list(
            @RequestParam UUID businessCustomerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BusinessCustomerLocationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BusinessCustomerLocationResponse> locations = service.list(businessCustomerId, search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "Business customer locations fetched successfully",
                PageResponse.from(locations)
        ));
    }

    @GetMapping("/by-customer-code")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<BusinessCustomerLocationResponse>>> listByCustomerCode(
            @RequestParam String customerCode,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BusinessCustomerLocationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BusinessCustomerLocationResponse> locations = service.listByCustomerCode(
                organizationId,
                customerCode,
                search,
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Business customer locations fetched successfully",
                PageResponse.from(locations)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<BusinessCustomerLocationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Location fetched successfully", service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<ApiResponse<BusinessCustomerLocationResponse>> create(
            @RequestParam UUID businessCustomerId,
            @Valid @RequestBody CreateBusinessCustomerLocationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Location created successfully",
                service.create(businessCustomerId, request, httpServletRequest)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    public ResponseEntity<ApiResponse<BusinessCustomerLocationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusinessCustomerLocationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Location updated successfully",
                service.update(id, request, httpServletRequest)
        ));
    }
}
