package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(3)
public class BranchAdminDashboardStrategy extends AbstractDashboardStrategy {
    public BranchAdminDashboardStrategy(DashboardAnalyticsRepository analytics) { super(analytics); }
    public boolean supports(DashboardContext context) {
        return context.hasRole("BRANCH_ADMIN") || (context.branchId() != null && context.businessCustomerId() == null);
    }
    public DashboardResponse build(DashboardContext context, int days) {
        return aggregate("BRANCH_ADMIN", context.organizationId(), context.branchId(), null, days);
    }
}

