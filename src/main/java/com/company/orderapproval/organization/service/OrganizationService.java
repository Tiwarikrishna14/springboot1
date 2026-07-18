package com.company.orderapproval.organization.service;

import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.dto.OrganizationResponse;
import com.company.orderapproval.organization.dto.UpdateOrganizationRequest;
import com.company.orderapproval.organization.dto.UpdateOrganizationStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public interface OrganizationService {
    Page<OrganizationResponse> list(Pageable pageable);

    OrganizationResponse get(UUID id);

    OrganizationResponse create(CreateOrganizationRequest request, HttpServletRequest servletRequest);

    OrganizationResponse update(UUID id, UpdateOrganizationRequest request, HttpServletRequest servletRequest);

    OrganizationResponse updateStatus(UUID id, UpdateOrganizationStatusRequest request, HttpServletRequest servletRequest);
}
