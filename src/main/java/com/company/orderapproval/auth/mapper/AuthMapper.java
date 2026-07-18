package com.company.orderapproval.auth.mapper;

import com.company.orderapproval.auth.dto.AuthUserResponse;
import com.company.orderapproval.user.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    default AuthUserResponse toAuthUserResponse(User user, List<String> roles, List<String> permissions) {
        return new AuthUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getOrganizationId(),
                roles,
                permissions
        );
    }
}
