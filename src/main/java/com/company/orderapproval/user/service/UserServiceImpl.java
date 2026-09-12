package com.company.orderapproval.user.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.branch.entity.Branch;
import com.company.orderapproval.branch.repository.BranchRepository;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocation;
import com.company.orderapproval.customer.location.repository.BusinessCustomerLocationRepository;
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
import com.company.orderapproval.user.entity.UserType;
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
    private final BranchRepository branchRepository;
    private final BusinessCustomerRepository businessCustomerRepository;
    private final BusinessCustomerLocationRepository businessCustomerLocationRepository;

    public UserServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository,
                           OrganizationRepository organizationRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService, BranchRepository branchRepository,
                           BusinessCustomerRepository businessCustomerRepository,
                           BusinessCustomerLocationRepository businessCustomerLocationRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.branchRepository = branchRepository;
        this.businessCustomerRepository = businessCustomerRepository;
        this.businessCustomerLocationRepository = businessCustomerLocationRepository;
    }

    @Override
    public Page<UserResponse> list(String search, UUID branchId, Pageable pageable) {
        UUID organizationFilter = SecurityContextHelper.isSuperAdmin()
                ? null
                : SecurityContextHelper.getCurrentOrganizationId();
        UUID branchFilter = resolveBranchFilter(branchId);
        UUID customerFilter = SecurityContextHelper.isSuperAdmin() ? null : currentUserBusinessCustomerId();
        UUID locationFilter = SecurityContextHelper.isSuperAdmin() ? null : currentUserBusinessCustomerLocationId();
        return userRepository.searchUsers(organizationFilter, branchFilter, customerFilter, locationFilter, search, pageable)
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
        user.setUserType(request.userType());
        validatePlacementForUserType(
                request.userType(),
                request.branchId(),
                request.businessCustomerId(),
                request.businessCustomerLocationId()
        );
        applyPlacement(user, organizationId, request.branchId(), request.businessCustomerId(), request.businessCustomerLocationId());
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
        if (!SecurityContextHelper.isSuperAdmin() && currentUserBranchId() != null
                && !currentUserBranchId().equals(user.getBranchId())) {
            throw new ForbiddenException("Cannot access users from another branch");
        }
        UUID currentCustomerId = currentUserBusinessCustomerId();
        if (!SecurityContextHelper.isSuperAdmin() && currentCustomerId != null
                && !currentCustomerId.equals(user.getBusinessCustomerId())) {
            throw new ForbiddenException("Cannot access users from another business customer");
        }
        return user;
    }

    private void applyPlacement(User user, UUID organizationId, UUID branchId, UUID businessCustomerId, UUID businessCustomerLocationId) {
        if (branchId == null && businessCustomerId == null && businessCustomerLocationId == null) return;
        if (branchId == null) throw new BadRequestException("branchId is required when assigning a business customer");
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!organizationId.equals(branch.getOrganizationId())) throw new ForbiddenException("Branch belongs to another organization");
        UUID currentBranchId = SecurityContextHelper.isSuperAdmin() ? null : currentUserBranchId();
        if (currentBranchId != null && !currentBranchId.equals(branchId)) {
            throw new ForbiddenException("Cannot assign a user to another branch");
        }
        user.setBranchId(branchId);
        if (businessCustomerId != null) {
            BusinessCustomer customer = businessCustomerRepository.findById(businessCustomerId).orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
            if (!branchId.equals(customer.getBranchId())) throw new ForbiddenException("Business customer belongs to another branch");
            user.setBusinessCustomerId(businessCustomerId);
        }
        if (businessCustomerLocationId != null) {
            if (businessCustomerId == null) throw new BadRequestException("businessCustomerId is required when assigning a location");
            BusinessCustomerLocation location = businessCustomerLocationRepository.findById(businessCustomerLocationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business customer location not found"));
            if (!branchId.equals(location.getBranchId()) || !businessCustomerId.equals(location.getBusinessCustomerId())) {
                throw new ForbiddenException("Business customer location belongs to another customer or branch");
            }
            user.setBusinessCustomerLocationId(location.getId());
        }
    }

    private void validatePlacementForUserType(UserType userType, UUID branchId, UUID businessCustomerId, UUID businessCustomerLocationId) {
        if (userType == null) {
            throw new BadRequestException("User type is required");
        }
        if (userType == UserType.EMPLOYEE) {
            if (branchId == null) {
                throw new BadRequestException("branchId is required for EMPLOYEE users");
            }
            if (businessCustomerId != null || businessCustomerLocationId != null) {
                throw new BadRequestException("EMPLOYEE users cannot be assigned to a business customer or location");
            }
        }
        if (userType == UserType.CUSTOMER) {
            if (branchId == null) {
                throw new BadRequestException("branchId is required for CUSTOMER users");
            }
            if (businessCustomerId == null) {
                throw new BadRequestException("businessCustomerId is required for CUSTOMER users");
            }
        }
    }

    private UUID resolveBranchFilter(UUID requestedBranchId) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return requestedBranchId;
        }

        UUID currentBranchId = currentUserBranchId();
        if (currentBranchId != null) {
            if (requestedBranchId != null && !requestedBranchId.equals(currentBranchId)) {
                throw new ForbiddenException("Cannot access users from another branch");
            }
            return currentBranchId;
        }

        if (requestedBranchId == null) {
            return null;
        }

        Branch branch = branchRepository.findById(requestedBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!SecurityContextHelper.getCurrentOrganizationId().equals(branch.getOrganizationId())) {
            throw new ForbiddenException("Cannot access users from another organization");
        }
        return requestedBranchId;
    }

    private UUID currentUserBranchId() {
        return userRepository.findById(SecurityContextHelper.getCurrentUserId()).map(User::getBranchId).orElse(null);
    }

    private UUID currentUserBusinessCustomerId() {
        return userRepository.findById(SecurityContextHelper.getCurrentUserId()).map(User::getBusinessCustomerId).orElse(null);
    }

    private UUID currentUserBusinessCustomerLocationId() {
        return userRepository.findById(SecurityContextHelper.getCurrentUserId()).map(User::getBusinessCustomerLocationId).orElse(null);
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
        if (("CUSTOMER".equals(role.getName()) || "CUSTOMER_ADMIN".equals(role.getName()))
                && user.getBusinessCustomerId() == null) {
            throw new BadRequestException("Customer users must be assigned to a business customer");
        }
        if ("EMPLOYEE".equals(role.getName()) && user.getBusinessCustomerId() != null) {
            throw new BadRequestException("Employees cannot be assigned to a business customer");
        }
        if ("BRANCH_ADMIN".equals(role.getName())
                && (user.getUserType() != UserType.EMPLOYEE || user.getBranchId() == null || user.getBusinessCustomerId() != null)) {
            throw new BadRequestException("Branch admin users must be EMPLOYEE users assigned to a branch");
        }
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
