package com.company.orderapproval.policy.service;

import com.company.orderapproval.common.exception.*;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.policy.dto.*;
import com.company.orderapproval.policy.repository.ApprovalPolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApprovalPolicyService {
    public static final String BUSINESS_CUSTOMER = "BUSINESS_CUSTOMER";
    public static final String ORDER_APPROVAL = "ORDER_APPROVAL";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ApprovalPolicyRepository policies;
    private final BusinessCustomerRepository customers;
    private final ObjectMapper objectMapper;
    private final Map<PolicyKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public ApprovalPolicyService(ApprovalPolicyRepository policies,
                                 BusinessCustomerRepository customers,
                                 ObjectMapper objectMapper) {
        this.policies = policies;
        this.customers = customers;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OrderApprovalPolicyConfig getOrderPolicy(UUID customerId) {
        assertCustomerAccess(customerId, false);
        return cachedOrderPolicy(customerId);
    }

    public OrderApprovalPolicyConfig resolveOrderPolicy(UUID customerId) {
        return cachedOrderPolicy(customerId);
    }

    @Transactional
    public OrderApprovalPolicyConfig upsertOrderPolicy(UUID customerId, OrderApprovalPolicyConfig config) {
        assertCustomerAccess(customerId, true);
        validate(config);
        try {
            policies.upsert(BUSINESS_CUSTOMER, customerId, ORDER_APPROVAL,
                    objectMapper.writeValueAsString(config), SecurityContextHelper.getCurrentUserId());
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid approval policy configuration");
        }
        cache.put(new PolicyKey(BUSINESS_CUSTOMER, customerId, ORDER_APPROVAL),
                new CacheEntry(config, Instant.now().plus(CACHE_TTL)));
        return config;
    }

    private OrderApprovalPolicyConfig cachedOrderPolicy(UUID customerId) {
        PolicyKey key = new PolicyKey(BUSINESS_CUSTOMER, customerId, ORDER_APPROVAL);
        CacheEntry existing = cache.get(key);
        if (existing != null && existing.expiresAt().isAfter(Instant.now())) {
            return existing.config();
        }
        OrderApprovalPolicyConfig config = policies
                .findByScopeTypeAndScopeIdAndPolicyTypeAndActiveTrue(BUSINESS_CUSTOMER, customerId, ORDER_APPROVAL)
                .map(policy -> objectMapper.convertValue(policy.getConfiguration(), OrderApprovalPolicyConfig.class))
                .orElseGet(OrderApprovalPolicyConfig::defaultPolicy);
        validate(config);
        cache.put(key, new CacheEntry(config, Instant.now().plus(CACHE_TTL)));
        return config;
    }

    private void validate(OrderApprovalPolicyConfig config) {
        if (config == null || config.approvalMode() == null || config.levels() == null || config.levels().isEmpty()) {
            throw new BadRequestException("Approval policy must contain at least one level");
        }
        Set<Integer> levels = new HashSet<>();
        for (ApprovalLevelPolicy level : config.levels()) {
            if (level == null || level.levelNumber() < 1 || level.minimumApprovers() < 1
                    || level.completionRule() == null || !levels.add(level.levelNumber())) {
                throw new BadRequestException("Approval levels must be unique and contain valid positive values");
            }
        }
        int expected = 1;
        for (int level : levels.stream().sorted().toList()) {
            if (level != expected++) {
                throw new BadRequestException("Approval levels must be continuous and start from 1");
            }
        }
    }

    private void assertCustomerAccess(UUID customerId, boolean update) {
        BusinessCustomer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
        if (!SecurityContextHelper.isSuperAdmin()
                && !customer.getOrganizationId().equals(SecurityContextHelper.getCurrentOrganizationId())) {
            throw new ForbiddenException("Cannot access another organization");
        }
        UUID currentCustomer = SecurityContextHelper.getCurrentBusinessCustomerId();
        if (currentCustomer != null && !currentCustomer.equals(customerId)) {
            throw new ForbiddenException("Cannot access another business customer");
        }
        if (update && !SecurityContextHelper.isSuperAdmin()
                && !SecurityContextHelper.hasRole("ORGANIZATION_ADMIN")
                && !SecurityContextHelper.hasRole("CUSTOMER_ADMIN")) {
            throw new ForbiddenException("Approval policy can only be updated by an administrator");
        }
    }

    private record PolicyKey(String scopeType, UUID scopeId, String policyType) {}
    private record CacheEntry(OrderApprovalPolicyConfig config, Instant expiresAt) {}
}

