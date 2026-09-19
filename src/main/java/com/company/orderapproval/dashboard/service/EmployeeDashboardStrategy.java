package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(5)
public class EmployeeDashboardStrategy extends AbstractDashboardStrategy {
    public EmployeeDashboardStrategy(DashboardAnalyticsRepository analytics) { super(analytics); }
    public boolean supports(DashboardContext context) { return context.organizationId() != null; }
    public DashboardResponse build(DashboardContext context, int days) {
        return aggregate("EMPLOYEE", context.organizationId(), context.branchId(), context.businessCustomerId(), days);
    }
}

