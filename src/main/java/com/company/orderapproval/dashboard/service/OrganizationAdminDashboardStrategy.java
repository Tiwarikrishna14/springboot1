package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(2)
public class OrganizationAdminDashboardStrategy extends AbstractDashboardStrategy {
    public OrganizationAdminDashboardStrategy(DashboardAnalyticsRepository analytics) { super(analytics); }
    public boolean supports(DashboardContext context) { return context.hasRole("ORGANIZATION_ADMIN"); }
    public DashboardResponse build(DashboardContext context, int days) {
        return aggregate("ORGANIZATION_ADMIN", context.organizationId(), null, null, days);
    }
}

