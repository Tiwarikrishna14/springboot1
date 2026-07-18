package com.company.orderapproval.user.service;

import com.company.orderapproval.user.dto.AssignRolesRequest;
import com.company.orderapproval.user.dto.CreateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserStatusRequest;
import com.company.orderapproval.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    Page<UserResponse> list(String search, Pageable pageable);

    UserResponse get(UUID id);

    UserResponse create(CreateUserRequest request, HttpServletRequest servletRequest);

    UserResponse update(UUID id, UpdateUserRequest request, HttpServletRequest servletRequest);

    UserResponse updateStatus(UUID id, UpdateUserStatusRequest request, HttpServletRequest servletRequest);

    UserResponse assignRoles(UUID id, AssignRolesRequest request, HttpServletRequest servletRequest);

    UserResponse removeRole(UUID id, UUID roleId, HttpServletRequest servletRequest);
}
