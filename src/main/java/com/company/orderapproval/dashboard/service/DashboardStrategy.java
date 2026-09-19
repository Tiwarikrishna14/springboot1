package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;

public interface DashboardStrategy {
    boolean supports(DashboardContext context);
    DashboardResponse build(DashboardContext context, int days);
}

