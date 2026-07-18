package com.company.orderapproval.role.service;

import com.company.orderapproval.role.dto.AssignPermissionsRequest;
import com.company.orderapproval.role.dto.CreateRoleRequest;
import com.company.orderapproval.role.dto.RoleResponse;
import com.company.orderapproval.role.dto.UpdateRoleRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoleService {
    Page<RoleResponse> list(String search, Pageable pageable);

    RoleResponse get(UUID id);

    RoleResponse create(CreateRoleRequest request, HttpServletRequest servletRequest);

    RoleResponse update(UUID id, UpdateRoleRequest request, HttpServletRequest servletRequest);

    RoleResponse assignPermissions(UUID id, AssignPermissionsRequest request, HttpServletRequest servletRequest);

    RoleResponse removePermission(UUID id, UUID permissionId, HttpServletRequest servletRequest);
}
