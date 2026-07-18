package com.company.orderapproval.permission.service;

import com.company.orderapproval.permission.dto.PermissionResponse;
import com.company.orderapproval.permission.mapper.PermissionMapper;
import com.company.orderapproval.permission.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<PermissionResponse> list() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
