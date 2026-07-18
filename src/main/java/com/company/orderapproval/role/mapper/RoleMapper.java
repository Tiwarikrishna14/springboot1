package com.company.orderapproval.role.mapper;

import com.company.orderapproval.role.dto.RoleResponse;
import com.company.orderapproval.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    RoleResponse toResponse(Role role);

    default RoleResponse toResponse(Role role, List<String> permissions) {
        RoleResponse base = toResponse(role);
        return new RoleResponse(
                base.id(),
                base.organizationId(),
                base.name(),
                base.description(),
                base.systemRole(),
                base.active(),
                base.createdAt(),
                base.updatedAt(),
                permissions
        );
    }
}
