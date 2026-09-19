package com.company.orderapproval.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.UUID;

public record DashboardResponse(
        String dashboardType,
        DashboardScope scope,
        LocalDate fromDate,
        LocalDate toDate,
        JsonNode overview,
        JsonNode orderStatusCounts,
        JsonNode approvalLoad,
        JsonNode organizationBreakdown,
        JsonNode branchBreakdown,
        JsonNode topCustomers,
        JsonNode topOrderLocations,
        JsonNode topOrders
) {
    public record DashboardScope(UUID organizationId, UUID branchId, UUID businessCustomerId) {}
}

