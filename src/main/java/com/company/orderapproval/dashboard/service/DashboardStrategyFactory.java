package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.dashboard.model.DashboardContext;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DashboardStrategyFactory {
    private final List<DashboardStrategy> strategies;
    public DashboardStrategyFactory(List<DashboardStrategy> strategies) { this.strategies = strategies; }
    public DashboardStrategy resolve(DashboardContext context) {
        return strategies.stream().filter(strategy -> strategy.supports(context)).findFirst()
                .orElseThrow(() -> new ForbiddenException("No dashboard is configured for this user"));
    }
}

