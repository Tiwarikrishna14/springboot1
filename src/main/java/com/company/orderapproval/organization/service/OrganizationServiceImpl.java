package com.company.orderapproval.organization.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.branch.repository.BranchRepository;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.response.DeleteValidationResponse;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.order.entity.OrderStatus;
import com.company.orderapproval.order.repository.OrderRepository;
import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.dto.OrganizationResponse;
import com.company.orderapproval.organization.dto.UpdateOrganizationRequest;
import com.company.orderapproval.organization.dto.UpdateOrganizationStatusRequest;
import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.entity.OrganizationStatus;
import com.company.orderapproval.organization.mapper.OrganizationMapper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.product.service.ProductRepository;
import com.company.orderapproval.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Set<OrderStatus> NOT_DELIVERED_ORDER_STATUSES = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.CREATED,
            OrderStatus.CHANGES_REQUESTED,
            OrderStatus.APPROVED,
            OrderStatus.SUBMITTED,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED
    );

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final BusinessCustomerRepository businessCustomerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   OrganizationMapper organizationMapper,
                                   AuditService auditService,
                                   UserRepository userRepository,
                                   BranchRepository branchRepository,
                                   BusinessCustomerRepository businessCustomerRepository,
                                   ProductRepository productRepository,
                                   OrderRepository orderRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.businessCustomerRepository = businessCustomerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<OrganizationResponse> list(Pageable pageable) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return organizationRepository.findByStatus(OrganizationStatus.ACTIVE, pageable).map(organizationMapper::toResponse);
        }
        UUID currentOrganizationId = SecurityContextHelper.getCurrentOrganizationId();
        return organizationRepository.findById(currentOrganizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .map(organization -> new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(organizationMapper.toResponse(organization)),
                        pageable,
                        1
                ))
                .orElse(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(),
                        pageable,
                        0
                ));
    }

    @Override
    public OrganizationResponse get(UUID id) {
        Organization organization = findAccessibleOrganization(id);
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ResourceNotFoundException("Organization not found");
        }
        return organizationMapper.toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, HttpServletRequest servletRequest) {
        if (!SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may create organizations");
        }
        String code = request.organizationCode().trim().toUpperCase(Locale.ROOT);
        if (organizationRepository.findByOrganizationCode(code).isPresent()) {
            throw new ConflictException("Organization code already exists");
        }
        Organization organization = organizationMapper.toEntity(request);
        organization.setOrganizationCode(code);
        organization.setCreatedBy(SecurityContextHelper.getCurrentUserId());
        organization.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        organizationRepository.save(organization);
        auditService.record(AuditActions.ORGANIZATION_CREATED, organization.getId(), SecurityContextHelper.getCurrentUserId(),
                "Organization", organization.getId(), "Organization created", null,
                Map.of("organizationCode", organization.getOrganizationCode(), "name", organization.getName()), servletRequest);
        return organizationMapper.toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request, HttpServletRequest servletRequest) {
        Organization organization = findAccessibleOrganization(id);
        Map<String, Object> oldValues = Map.of(
                "name", organization.getName(),
                "organizationType", organization.getOrganizationType().name(),
                "email", organization.getEmail() == null ? "" : organization.getEmail(),
                "phone", organization.getPhone() == null ? "" : organization.getPhone()
        );
        organization.setName(request.name().trim());
        organization.setOrganizationType(request.organizationType());
        organization.setEmail(request.email());
        organization.setPhone(request.phone());
        organization.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        organizationRepository.save(organization);
        auditService.record(AuditActions.ORGANIZATION_UPDATED, organization.getId(), SecurityContextHelper.getCurrentUserId(),
                "Organization", organization.getId(), "Organization updated", oldValues,
                Map.of("name", organization.getName()), servletRequest);
        return organizationMapper.toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse updateStatus(UUID id,
                                             UpdateOrganizationStatusRequest request,
                                             HttpServletRequest servletRequest) {
        if (!SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may change organization status");
        }
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Map<String, Object> oldValues = Map.of("status", organization.getStatus().name());
        organization.setStatus(request.status());
        organization.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        organizationRepository.save(organization);
        auditService.record(AuditActions.ORGANIZATION_UPDATED, organization.getId(), SecurityContextHelper.getCurrentUserId(),
                "Organization", organization.getId(), "Organization status updated", oldValues,
                Map.of("status", organization.getStatus().name()), servletRequest);
        return organizationMapper.toResponse(organization);
    }

    @Override
    public DeleteValidationResponse validateDelete(UUID id) {
        if (!SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may deactivate organizations");
        }
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Map<String, Long> counts = organizationDeleteCounts(organization.getId());
        List<String> warnings = organizationDeleteWarnings(counts);
        return new DeleteValidationResponse(
                !warnings.isEmpty(),
                warnings.isEmpty()
                        ? "No organization mappings found. Organization can be deactivated safely."
                        : "Organization has associated mappings. User confirmation is required before deactivation.",
                warnings,
                counts
        );
    }

    @Override
    @Transactional
    public OrganizationResponse delete(UUID id, boolean force, HttpServletRequest servletRequest) {
        if (!SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may deactivate organizations");
        }
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Map<String, Long> counts = organizationDeleteCounts(organization.getId());
        List<String> warnings = organizationDeleteWarnings(counts);

        if (!warnings.isEmpty() && !force) {
            throw new BadRequestException(
                    "Cannot deactivate organization without confirmation because "
                            + String.join(" and ", warnings)
                            + ". Call DELETE with force=true after user accepts the warning."
            );
        }

        Map<String, Object> oldValues = Map.of("status", organization.getStatus().name());
        organization.setStatus(OrganizationStatus.INACTIVE);
        organization.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        organizationRepository.save(organization);
        auditService.record(AuditActions.ORGANIZATION_UPDATED, organization.getId(), SecurityContextHelper.getCurrentUserId(),
                "Organization", organization.getId(), "Organization deactivated", oldValues,
                Map.of("status", organization.getStatus().name()), servletRequest);
        return organizationMapper.toResponse(organization);
    }

    private Map<String, Long> organizationDeleteCounts(UUID organizationId) {
        List<String> customerCodes = businessCustomerRepository.findCustomerCodesByOrganizationId(organizationId);
        long productCount = customerCodes.isEmpty()
                ? 0
                : productRepository.countByCustomerSellCodeIn(customerCodes);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", userRepository.countByOrganizationId(organizationId));
        counts.put("branches", branchRepository.countByOrganizationId(organizationId));
        counts.put("businessCustomers", businessCustomerRepository.countByOrganizationId(organizationId));
        counts.put("products", productCount);
        counts.put("notDeliveredOrders", orderRepository.countByOrganizationIdAndStatusIn(
                organizationId,
                NOT_DELIVERED_ORDER_STATUSES
        ));
        return counts;
    }

    private List<String> organizationDeleteWarnings(Map<String, Long> counts) {
        List<String> warnings = new ArrayList<>();
        long userCount = counts.get("users");
        long branchCount = counts.get("branches");
        long customerCount = counts.get("businessCustomers");
        long productCount = counts.get("products");
        long orderCount = counts.get("notDeliveredOrders");

        if (userCount > 0) {
            warnings.add(userCount + " user(s) are mapped to this organization");
        }
        if (branchCount > 0) {
            warnings.add(branchCount + " branch(es) are mapped to this organization");
        }
        if (customerCount > 0) {
            warnings.add(customerCount + " business customer(s) are mapped to this organization");
        }
        if (productCount > 0) {
            warnings.add(productCount + " product(s) are mapped through this organization's business customers");
        }
        if (orderCount > 0) {
            warnings.add(orderCount + " order(s) are not delivered for this organization");
        }
        return warnings;
    }

    private Organization findAccessibleOrganization(UUID id) {
        if (!SecurityContextHelper.isSuperAdmin()
                && !SecurityContextHelper.getCurrentOrganizationId().equals(id)) {
            throw new ForbiddenException("Cannot access another organization");
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
