package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(1)
public class SuperAdminDashboardStrategy extends AbstractDashboardStrategy {
    public SuperAdminDashboardStrategy(DashboardAnalyticsRepository analytics) { super(analytics); }
    public boolean supports(DashboardContext context) { return context.hasRole("SUPER_ADMIN"); }
    public DashboardResponse build(DashboardContext context, int days) {
        return aggregate("SUPER_ADMIN", null, null, null, days);
    }
}

