package com.company.orderapproval.permission.service;

import com.company.orderapproval.permission.dto.PermissionResponse;
import com.company.orderapproval.permission.mapper.PermissionMapper;
import com.company.orderapproval.permission.repository.PermissionRepository;
import com.company.orderapproval.role.service.RoleDelegationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final RoleDelegationService roleDelegationService;

    public PermissionServiceImpl(PermissionRepository permissionRepository,
                                 PermissionMapper permissionMapper,
                                 RoleDelegationService roleDelegationService) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
        this.roleDelegationService = roleDelegationService;
    }

    @Override
    public List<PermissionResponse> list() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    public List<PermissionResponse> assignable() {
        return roleDelegationService.getAssignablePermissions(roleDelegationService.currentUser())
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
