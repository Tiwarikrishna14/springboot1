package com.company.orderapproval.user.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.user.dto.AssignRolesRequest;
import com.company.orderapproval.user.dto.CreateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserRequest;
import com.company.orderapproval.user.dto.UpdateUserStatusRequest;
import com.company.orderapproval.user.dto.UserResponse;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.entity.UserRole;
import com.company.orderapproval.user.entity.UserStatus;
import com.company.orderapproval.user.mapper.UserMapper;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository,
                           OrganizationRepository organizationRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public Page<UserResponse> list(String search, Pageable pageable) {
        UUID organizationFilter = SecurityContextHelper.isSuperAdmin()
                ? null
                : SecurityContextHelper.getCurrentOrganizationId();
        return userRepository.searchUsers(organizationFilter, search, pageable)
                .map(this::toResponse);
    }

    @Override
    public UserResponse get(UUID id) {
        return toResponse(findAccessibleUser(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request, HttpServletRequest servletRequest) {
        UUID organizationId = resolveOrganizationId(request.organizationId());
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setOrganizationId(organizationId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setCreatedBy(SecurityContextHelper.getCurrentUserId());
        user.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        userRepository.save(user);

        if (request.roleIds() != null) {
            request.roleIds().forEach(roleId -> assignRole(user, roleId));
        }

        auditService.record(AuditActions.USER_CREATED, user.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "User", user.getId(), "User created", null,
                Map.of("email", user.getEmail(), "organizationId", user.getOrganizationId().toString()), servletRequest);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        Map<String, Object> oldValues = Map.of(
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail()
        );
        String newEmail = request.email() == null || request.email().isBlank()
                ? user.getEmail()
                : normalizeEmail(request.email());
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email already exists");
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(newEmail);
        user.setPhone(request.phone());
        user.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        userRepository.save(user);
        auditService.record(AuditActions.USER_UPDATED, user.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "User", user.getId(), "User updated", oldValues,
                Map.of("email", user.getEmail()), servletRequest);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, UpdateUserStatusRequest request, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        Map<String, Object> oldValues = Map.of("status", user.getStatus().name());
        user.setStatus(request.status());
        if (request.status() != UserStatus.LOCKED) {
            user.setAccountLockedUntil(null);
        }
        user.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        userRepository.save(user);
        auditService.record(AuditActions.USER_STATUS_CHANGED, user.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "User", user.getId(), "User status changed", oldValues,
                Map.of("status", user.getStatus().name()), servletRequest);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        request.roleIds().forEach(roleId -> assignRole(user, roleId));
        auditService.record(AuditActions.ROLE_ASSIGNED, user.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "User", user.getId(), "Roles assigned", null,
                Map.of("roleIds", request.roleIds().stream().map(UUID::toString).toList()), servletRequest);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse removeRole(UUID id, UUID roleId, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        validateRoleAssignableToUser(user, role);
        userRoleRepository.deleteByUserIdAndRoleId(user.getId(), role.getId());
        auditService.record(AuditActions.ROLE_REMOVED, user.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "User", user.getId(), "Role removed", Map.of("roleId", roleId.toString()), null, servletRequest);
        return toResponse(user);
    }

    private User findAccessibleUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!SecurityContextHelper.isSuperAdmin()
                && !SecurityContextHelper.getCurrentOrganizationId().equals(user.getOrganizationId())) {
            throw new ForbiddenException("Cannot access users from another organization");
        }
        return user;
    }

    private UUID resolveOrganizationId(UUID requestedOrganizationId) {
        if (SecurityContextHelper.isSuperAdmin()) {
            if (requestedOrganizationId == null) {
                throw new BadRequestException("organizationId is required for super admin user creation");
            }
            return requestedOrganizationId;
        }
        return SecurityContextHelper.getCurrentOrganizationId();
    }

    private void assignRole(User user, UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        validateRoleAssignableToUser(user, role);
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }

    private void validateRoleAssignableToUser(User user, Role role) {
        if ("SUPER_ADMIN".equals(role.getName()) && !SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may assign SUPER_ADMIN");
        }
        if (role.getOrganizationId() != null && !role.getOrganizationId().equals(user.getOrganizationId())) {
            throw new ForbiddenException("Cannot assign role from another organization");
        }
        if (!SecurityContextHelper.isSuperAdmin() && role.getOrganizationId() != null
                && !SecurityContextHelper.getCurrentOrganizationId().equals(role.getOrganizationId())) {
            throw new ForbiddenException("Cannot assign role from another organization");
        }
        if (!role.isActive()) {
            throw new BadRequestException("Role is inactive");
        }
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(
                user,
                userRoleRepository.findRoleNamesByUserId(user.getId()),
                userRoleRepository.findPermissionCodesByUserId(user.getId())
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
