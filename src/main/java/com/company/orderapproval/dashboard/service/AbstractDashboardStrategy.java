package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import java.util.UUID;

abstract class AbstractDashboardStrategy implements DashboardStrategy {
    protected final DashboardAnalyticsRepository analytics;
    protected AbstractDashboardStrategy(DashboardAnalyticsRepository analytics) { this.analytics = analytics; }
    protected DashboardResponse aggregate(String type, UUID organizationId, UUID branchId, UUID customerId, int days) {
        return analytics.aggregate(type, organizationId, branchId, customerId, days);
    }
}

