package com.company.orderapproval.user.service;

import com.company.orderapproval.audit.repository.AuditLogRepository;
import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.auth.repository.PasswordResetTokenRepository;
import com.company.orderapproval.auth.repository.RefreshTokenRepository;
import com.company.orderapproval.branch.entity.BranchStatus;
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
import com.company.orderapproval.customer.entity.BusinessCustomerStatus;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocation;
import com.company.orderapproval.customer.location.repository.BusinessCustomerLocationRepository;
import com.company.orderapproval.order.repository.OrderApproverRepository;
import com.company.orderapproval.order.repository.OrderRepository;
import com.company.orderapproval.organization.entity.OrganizationStatus;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.role.service.RoleDelegationService;
import com.company.orderapproval.user.dto.AssignRolesRequest;
import com.company.orderapproval.user.dto.ApproverUserResponse;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;
    private final OrderApproverRepository orderApproverRepository;
    private final RoleDelegationService roleDelegationService;

    public UserServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository,
                           OrganizationRepository organizationRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService, BranchRepository branchRepository,
                           BusinessCustomerRepository businessCustomerRepository,
                           BusinessCustomerLocationRepository businessCustomerLocationRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           AuditLogRepository auditLogRepository,
                           OrderRepository orderRepository,
                           OrderApproverRepository orderApproverRepository,
                           RoleDelegationService roleDelegationService) {
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
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.orderRepository = orderRepository;
        this.orderApproverRepository = orderApproverRepository;
        this.roleDelegationService = roleDelegationService;
    }

    @Override
    public Page<UserResponse> list(String search,
                                   UUID branchId,
                                   UUID businessCustomerId,
                                   UUID businessCustomerLocationId,
                                   UserStatus status,
                                   List<String> roles,
                                   Pageable pageable) {
        if (status == UserStatus.INACTIVE) {
            return Page.empty(pageable);
        }
        User currentUser = currentUser();
        UUID organizationFilter = SecurityContextHelper.isSuperAdmin()
                ? null
                : currentUser.getOrganizationId();
        UUID branchFilter = resolveBranchFilter(branchId, currentUser);
        UUID customerFilter = resolveBusinessCustomerFilter(businessCustomerId, branchFilter, currentUser);
        UUID locationFilter = resolveLocationFilter(businessCustomerLocationId, customerFilter, branchFilter, currentUser);
        List<String> normalizedRoles = normalizeRoles(roles);
        boolean roleNamesEmpty = normalizedRoles.isEmpty();
        List<String> roleNames = roleNamesEmpty ? List.of("__NO_ROLE_FILTER__") : normalizedRoles;
        return userRepository.searchUsers(
                        organizationFilter,
                        branchFilter,
                        customerFilter,
                        locationFilter,
                        status,
                        UserStatus.INACTIVE,
                        roleNamesEmpty,
                        roleNames,
                        search,
                        pageable
                )
                .map(this::toResponse);
    }

    @Override
    public List<ApproverUserResponse> approvers(UUID businessCustomerId) {
        if (businessCustomerId == null) {
            throw new BadRequestException("businessCustomerId is required");
        }
        BusinessCustomer customer = businessCustomerRepository.findById(businessCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
        if (customer.getStatus() != BusinessCustomerStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer not found");
        }
        assertBusinessCustomerAccessible(customer, currentUser());

        return userRepository.findApproverUsersByBusinessCustomerId(
                        businessCustomerId,
                        UserStatus.ACTIVE,
                        "ORDER_APPROVE"
                )
                .stream()
                .map(user -> new ApproverUserResponse(user.getId(), fullName(user), user.getEmail()))
                .toList();
    }

    @Override
    public UserResponse get(UUID id) {
        User user = findAccessibleUser(id);
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResourceNotFoundException("User not found");
        }
        return toResponse(user);
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
    public UserResponse delete(UUID id, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        UUID currentUserId = SecurityContextHelper.getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot delete your own user account");
        }

        Map<String, Object> oldValues = Map.of("status", user.getStatus().name());
        if (hasOrderMapping(user.getId())) {
            user.setStatus(UserStatus.INACTIVE);
            user.setAccountLockedUntil(null);
            user.setUpdatedBy(currentUserId);
            userRepository.save(user);
            refreshTokenRepository.revokeActiveTokensByUserId(user.getId(), Instant.now());

            auditService.record(AuditActions.USER_DELETED, user.getOrganizationId(), currentUserId,
                    "User", user.getId(), "User soft deleted because order history exists", oldValues,
                    Map.of("status", user.getStatus().name()), servletRequest);
            return toResponse(user);
        }

        UserResponse deletedUser = toResponse(user);
        UUID organizationId = user.getOrganizationId();
        UUID deletedUserId = user.getId();
        refreshTokenRepository.deleteByUserId(deletedUserId);
        passwordResetTokenRepository.deleteByUserId(deletedUserId);
        userRoleRepository.deleteByUserId(deletedUserId);
        auditLogRepository.clearUserReferences(deletedUserId);
        userRepository.delete(user);

        auditService.record(AuditActions.USER_DELETED, organizationId, currentUserId,
                "User", deletedUserId, "User hard deleted because no order history exists", oldValues,
                Map.of("deleted", true), servletRequest);
        return deletedUser;
    }

    @Override
    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request, HttpServletRequest servletRequest) {
        User user = findAccessibleUser(id);
        User actor = currentUser();
        roleDelegationService.validateTargetUserScope(actor, user);
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
        User actor = currentUser();
        roleDelegationService.validateTargetUserScope(actor, user);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleDelegationService.validateCanAssignRole(actor, role);
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
        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new ResourceNotFoundException("Branch not found");
        }
        if (!organizationId.equals(branch.getOrganizationId())) throw new ForbiddenException("Branch belongs to another organization");
        UUID currentBranchId = SecurityContextHelper.isSuperAdmin() ? null : currentUserBranchId();
        if (currentBranchId != null && !currentBranchId.equals(branchId)) {
            throw new ForbiddenException("Cannot assign a user to another branch");
        }
        user.setBranchId(branchId);
        if (businessCustomerId != null) {
            BusinessCustomer customer = businessCustomerRepository.findById(businessCustomerId).orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
            if (customer.getStatus() != BusinessCustomerStatus.ACTIVE) {
                throw new ResourceNotFoundException("Business customer not found");
            }
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

    private UUID resolveBranchFilter(UUID requestedBranchId, User currentUser) {
        Branch requestedBranch = null;
        if (requestedBranchId != null) {
            requestedBranch = branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            if (requestedBranch.getStatus() != BranchStatus.ACTIVE) {
                throw new ResourceNotFoundException("Branch not found");
            }
        }

        if (SecurityContextHelper.isSuperAdmin()) {
            return requestedBranchId;
        }

        UUID currentBranchId = currentUser.getBranchId();
        if (currentBranchId != null) {
            Branch currentBranch = branchRepository.findById(currentBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            if (currentBranch.getStatus() != BranchStatus.ACTIVE) {
                throw new ResourceNotFoundException("Branch not found");
            }
            if (requestedBranchId != null && !requestedBranchId.equals(currentBranchId)) {
                throw new ForbiddenException("Cannot access users from another branch");
            }
            return currentBranchId;
        }

        if (requestedBranchId == null) {
            return null;
        }

        if (!SecurityContextHelper.getCurrentOrganizationId().equals(requestedBranch.getOrganizationId())) {
            throw new ForbiddenException("Cannot access users from another organization");
        }
        return requestedBranchId;
    }

    private UUID resolveBusinessCustomerFilter(UUID requestedBusinessCustomerId, UUID branchFilter, User currentUser) {
        BusinessCustomer requestedCustomer = null;
        if (requestedBusinessCustomerId != null) {
            requestedCustomer = businessCustomerRepository.findById(requestedBusinessCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
            if (requestedCustomer.getStatus() != BusinessCustomerStatus.ACTIVE) {
                throw new ResourceNotFoundException("Business customer not found");
            }
        }

        if (SecurityContextHelper.isSuperAdmin()) {
            return requestedBusinessCustomerId;
        }

        UUID currentBusinessCustomerId = currentUser.getBusinessCustomerId();
        if (currentBusinessCustomerId != null) {
            BusinessCustomer currentCustomer = businessCustomerRepository.findById(currentBusinessCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
            if (currentCustomer.getStatus() != BusinessCustomerStatus.ACTIVE) {
                throw new ResourceNotFoundException("Business customer not found");
            }
            if (requestedBusinessCustomerId != null && !requestedBusinessCustomerId.equals(currentBusinessCustomerId)) {
                throw new ForbiddenException("Cannot access users from another business customer");
            }
            return currentBusinessCustomerId;
        }

        if (requestedBusinessCustomerId == null) {
            return null;
        }

        assertBusinessCustomerAccessible(requestedCustomer, currentUser);
        if (branchFilter != null && !branchFilter.equals(requestedCustomer.getBranchId())) {
            throw new ForbiddenException("Business customer belongs to another branch");
        }
        return requestedBusinessCustomerId;
    }

    private UUID resolveLocationFilter(UUID requestedLocationId,
                                       UUID businessCustomerFilter,
                                       UUID branchFilter,
                                       User currentUser) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return requestedLocationId;
        }

        UUID currentLocationId = currentUser.getBusinessCustomerLocationId();
        if (currentLocationId != null) {
            if (requestedLocationId != null && !requestedLocationId.equals(currentLocationId)) {
                throw new ForbiddenException("Cannot access users from another business customer location");
            }
            return currentLocationId;
        }

        if (requestedLocationId == null) {
            return null;
        }

        BusinessCustomerLocation location = businessCustomerLocationRepository.findById(requestedLocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer location not found"));
        if (!currentUser.getOrganizationId().equals(location.getOrganizationId())) {
            throw new ForbiddenException("Cannot access users from another organization");
        }
        if (branchFilter != null && !branchFilter.equals(location.getBranchId())) {
            throw new ForbiddenException("Business customer location belongs to another branch");
        }
        if (businessCustomerFilter != null && !businessCustomerFilter.equals(location.getBusinessCustomerId())) {
            throw new ForbiddenException("Business customer location belongs to another customer");
        }
        return requestedLocationId;
    }

    private void assertBusinessCustomerAccessible(BusinessCustomer customer, User currentUser) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return;
        }
        if (!Objects.equals(currentUser.getOrganizationId(), customer.getOrganizationId())) {
            throw new ForbiddenException("Cannot access business customer from another organization");
        }
        if (currentUser.getBranchId() != null && !Objects.equals(currentUser.getBranchId(), customer.getBranchId())) {
            throw new ForbiddenException("Cannot access business customer from another branch");
        }
        if (currentUser.getBusinessCustomerId() != null
                && !Objects.equals(currentUser.getBusinessCustomerId(), customer.getId())) {
            throw new ForbiddenException("Cannot access another business customer");
        }
    }

    private User currentUser() {
        return userRepository.findById(SecurityContextHelper.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .flatMap(role -> Arrays.stream(role.split(",")))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .distinct()
                .toList();
    }

    private String fullName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName())
                + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private boolean hasOrderMapping(UUID userId) {
        return orderRepository.countByCreatedBy(userId) > 0
                || orderApproverRepository.countByUserId(userId) > 0;
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
        User actor = currentUser();
        roleDelegationService.validateTargetUserScope(actor, user);
        roleDelegationService.validateCanAssignRole(actor, role);
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
                organizationName(user.getOrganizationId()),
                branchName(user.getBranchId()),
                businessCustomerName(user.getBusinessCustomerId()),
                userRoleRepository.findRoleNamesByUserId(user.getId()),
                userRoleRepository.findPermissionCodesByUserId(user.getId())
        );
    }

    private String organizationName(UUID organizationId) {
        if (organizationId == null) {
            return null;
        }
        return organizationRepository.findById(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .map(organization -> organization.getName())
                .orElse(null);
    }

    private String branchName(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                .map(branch -> branch.getName())
                .orElse(null);
    }

    private String businessCustomerName(UUID businessCustomerId) {
        if (businessCustomerId == null) {
            return null;
        }
        return businessCustomerRepository.findById(businessCustomerId)
                .filter(customer -> customer.getStatus() == BusinessCustomerStatus.ACTIVE)
                .map(customer -> customer.getName())
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
