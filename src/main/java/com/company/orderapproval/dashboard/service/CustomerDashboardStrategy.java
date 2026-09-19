package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import com.company.orderapproval.dashboard.repository.DashboardAnalyticsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(4)
public class CustomerDashboardStrategy extends AbstractDashboardStrategy {
    public CustomerDashboardStrategy(DashboardAnalyticsRepository analytics) { super(analytics); }
    public boolean supports(DashboardContext context) { return context.businessCustomerId() != null; }
    public DashboardResponse build(DashboardContext context, int days) {
        String type = context.hasRole("CUSTOMER_ADMIN") ? "CUSTOMER_ADMIN" : "CUSTOMER_USER";
        return aggregate(type, context.organizationId(), context.branchId(), context.businessCustomerId(), days);
    }
}

