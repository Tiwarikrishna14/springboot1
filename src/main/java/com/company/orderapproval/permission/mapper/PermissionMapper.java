package com.company.orderapproval.permission.mapper;

import com.company.orderapproval.permission.dto.PermissionResponse;
import com.company.orderapproval.permission.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toResponse(Permission permission);
}
