package com.company.orderapproval.permission.service;

import com.company.orderapproval.permission.dto.PermissionResponse;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> list();
}
