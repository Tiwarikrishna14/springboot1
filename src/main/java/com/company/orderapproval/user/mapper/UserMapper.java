package com.company.orderapproval.user.mapper;

import com.company.orderapproval.user.dto.UserResponse;
import com.company.orderapproval.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    UserResponse toResponse(User user);

    default UserResponse toResponse(User user, List<String> roles, List<String> permissions) {
        UserResponse base = toResponse(user);
        return new UserResponse(
                base.id(),
                base.organizationId(),
                base.branchId(),
                base.businessCustomerId(),
                base.businessCustomerLocationId(),
                base.userType(),
                base.firstName(),
                base.lastName(),
                base.email(),
                base.phone(),
                base.status(),
                base.emailVerified(),
                base.failedLoginAttempts(),
                base.accountLockedUntil(),
                base.lastLoginAt(),
                base.createdAt(),
                base.updatedAt(),
                base.createdBy(),
                base.updatedBy(),
                roles,
                permissions
        );
    }
}
