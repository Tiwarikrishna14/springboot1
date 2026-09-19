package com.company.orderapproval.dashboard.controller;

import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardResponse>> get(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched successfully",
                service.getDashboard(days)));
    }
}
