package com.company.orderapproval.order.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.common.response.PageResponse;
import com.company.orderapproval.order.dto.ApprovalActionRequest;
import com.company.orderapproval.order.dto.CreateOrderRequest;
import com.company.orderapproval.order.dto.OrderResponse;
import com.company.orderapproval.order.dto.SupplierActionRequest;
import com.company.orderapproval.order.dto.UpdateOrderRequest;
import com.company.orderapproval.order.entity.OrderStatus;
import com.company.orderapproval.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping({"/api/v1/orders", "/api/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "List orders")
    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) UUID businessCustomerId,
            @RequestParam(required = false) String orderNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Orders fetched successfully",
                PageResponse.from(orderService.list(status, createdBy, businessCustomerId, orderNumber, pageable))
        ));
    }

    @Operation(summary = "Get order by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public ResponseEntity<ApiResponse<OrderResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", orderService.get(id)));
    }

    @Operation(summary = "Create order")
    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request,
                                                             HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", orderService.create(request, servletRequest)));
    }

    @Operation(summary = "Update order")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public ResponseEntity<ApiResponse<OrderResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateOrderRequest request,
                                                             HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully", orderService.update(id, request, servletRequest)));
    }

    @Operation(summary = "Record order approval action")
    @PostMapping("/{id}/approval")
    @PreAuthorize("hasAnyAuthority('ORDER_APPROVE', 'ORDER_REJECT', 'ORDER_REVIEW')")
    public ResponseEntity<ApiResponse<OrderResponse>> approvalAction(@PathVariable UUID id,
                                                                     @Valid @RequestBody ApprovalActionRequest request,
                                                                     HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order approval action recorded successfully",
                orderService.actOnApproval(id, request, servletRequest)
        ));
    }

    @Operation(summary = "Submit order")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('ORDER_SUBMIT', 'ORDER_APPROVE')")
    public ResponseEntity<ApiResponse<OrderResponse>> submit(@PathVariable UUID id,
                                                             HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Order submitted successfully", orderService.submit(id, servletRequest)));
    }

    @Operation(summary = "Record supplier order action")
    @PostMapping("/{id}/supplier-action")
    @PreAuthorize("(hasRole('SUPER_ADMIN') or hasRole('ORGANIZATION_ADMIN') or hasRole('BRANCH_ADMIN')) and hasAnyAuthority('ORDER_UPDATE', 'ORDER_REJECT')")
    public ResponseEntity<ApiResponse<OrderResponse>> supplierAction(@PathVariable UUID id,
                                                                     @Valid @RequestBody SupplierActionRequest request,
                                                                     HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Supplier order action recorded successfully",
                orderService.supplierAction(id, request, servletRequest)
        ));
    }
}
