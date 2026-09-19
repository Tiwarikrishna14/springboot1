package com.company.orderapproval.dashboard.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.company.orderapproval.dashboard.model.DashboardContext;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DashboardService {
    private final DashboardStrategyFactory factory;
    private final Map<DashboardCacheKey, DashboardCacheEntry> cache = new ConcurrentHashMap<>();
    public DashboardService(DashboardStrategyFactory factory) { this.factory = factory; }
    public DashboardResponse getDashboard(int days) {
        if (days < 1 || days > 365) throw new BadRequestException("days must be between 1 and 365");
        DashboardContext context = DashboardContext.from(SecurityContextHelper.currentUser());
        DashboardCacheKey key = new DashboardCacheKey(
                context.organizationId(), context.branchId(), context.businessCustomerId(), context.roles(), days);
        DashboardCacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.response();
        DashboardResponse response = factory.resolve(context).build(context, days);
        cache.put(key, new DashboardCacheEntry(response, Instant.now().plusSeconds(30)));
        return response;
    }

    private record DashboardCacheKey(java.util.UUID organizationId, java.util.UUID branchId,
                                     java.util.UUID customerId, java.util.List<String> roles, int days) {}
    private record DashboardCacheEntry(DashboardResponse response, Instant expiresAt) {}
}
