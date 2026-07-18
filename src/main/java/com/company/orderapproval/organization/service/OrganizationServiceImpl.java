package com.company.orderapproval.organization.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.dto.OrganizationResponse;
import com.company.orderapproval.organization.dto.UpdateOrganizationRequest;
import com.company.orderapproval.organization.dto.UpdateOrganizationStatusRequest;
import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.mapper.OrganizationMapper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   OrganizationMapper organizationMapper,
                                   AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.auditService = auditService;
    }

    @Override
    public Page<OrganizationResponse> list(Pageable pageable) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return organizationRepository.findAll(pageable).map(organizationMapper::toResponse);
        }
        UUID currentOrganizationId = SecurityContextHelper.getCurrentOrganizationId();
        return organizationRepository.findById(currentOrganizationId)
                .map(organization -> new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(organizationMapper.toResponse(organization)),
                        pageable,
                        1
                ))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    @Override
    public OrganizationResponse get(UUID id) {
        Organization organization = findAccessibleOrganization(id);
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

    private Organization findAccessibleOrganization(UUID id) {
        if (!SecurityContextHelper.isSuperAdmin()
                && !SecurityContextHelper.getCurrentOrganizationId().equals(id)) {
            throw new ForbiddenException("Cannot access another organization");
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
