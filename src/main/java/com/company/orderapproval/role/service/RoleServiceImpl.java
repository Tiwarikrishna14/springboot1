package com.company.orderapproval.role.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.permission.entity.Permission;
import com.company.orderapproval.permission.repository.PermissionRepository;
import com.company.orderapproval.role.dto.AssignPermissionsRequest;
import com.company.orderapproval.role.dto.CreateRoleRequest;
import com.company.orderapproval.role.dto.RoleResponse;
import com.company.orderapproval.role.dto.UpdateRoleRequest;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.entity.RolePermission;
import com.company.orderapproval.role.mapper.RoleMapper;
import com.company.orderapproval.role.repository.RolePermissionRepository;
import com.company.orderapproval.role.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleMapper roleMapper;
    private final AuditService auditService;
    private final RoleDelegationService roleDelegationService;

    public RoleServiceImpl(RoleRepository roleRepository,
                           RolePermissionRepository rolePermissionRepository,
                           PermissionRepository permissionRepository,
                           OrganizationRepository organizationRepository,
                           RoleMapper roleMapper,
                           AuditService auditService,
                           RoleDelegationService roleDelegationService) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.organizationRepository = organizationRepository;
        this.roleMapper = roleMapper;
        this.auditService = auditService;
        this.roleDelegationService = roleDelegationService;
    }

    @Override
    public Page<RoleResponse> list(String search, Pageable pageable) {
        boolean includeAll = SecurityContextHelper.isSuperAdmin();
        UUID organizationId = includeAll ? null : SecurityContextHelper.getCurrentOrganizationId();
        return roleRepository.searchRoles(organizationId, includeAll, search, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<RoleResponse> assignable() {
        return roleDelegationService.getAssignableRoles(roleDelegationService.currentUser())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RoleResponse get(UUID id) {
        return toResponse(findAccessibleRole(id));
    }

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest request, HttpServletRequest servletRequest) {
        UUID organizationId = resolveRoleOrganizationId(request);
        String name = normalizeName(request.name());
        if (roleExists(name, organizationId)) {
            throw new ConflictException("Role already exists");
        }

        Role role = new Role();
        role.setOrganizationId(organizationId);
        role.setName(name);
        role.setDescription(request.description());
        role.setSystemRole(request.systemRole() && SecurityContextHelper.isSuperAdmin());
        role.setLevel(roleDelegationService.resolveRoleLevelForCreate(roleDelegationService.currentUser(), request.level()));
        role.setActive(true);
        roleRepository.save(role);
        auditService.record(AuditActions.ROLE_CREATED, organizationId, SecurityContextHelper.getCurrentUserId(),
                "Role", role.getId(), "Role created", null,
                Map.of("name", role.getName()), servletRequest);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request, HttpServletRequest servletRequest) {
        Role role = findAccessibleRole(id);
        enforceSystemRoleWrite(role);
        roleDelegationService.validateCanManageRole(roleDelegationService.currentUser(), role);
        String name = normalizeName(request.name());
        if (!name.equals(role.getName()) && roleExists(name, role.getOrganizationId())) {
            throw new ConflictException("Role already exists");
        }
        Map<String, Object> oldValues = Map.of(
                "name", role.getName(),
                "description", role.getDescription() == null ? "" : role.getDescription(),
                "active", role.isActive()
        );
        role.setName(name);
        role.setDescription(request.description());
        if (request.level() != null) {
            roleDelegationService.validateRoleLevelForWrite(roleDelegationService.currentUser(), request.level());
            role.setLevel(request.level());
        }
        role.setActive(request.active());
        roleRepository.save(role);
        auditService.record(AuditActions.ROLE_UPDATED, role.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "Role", role.getId(), "Role updated", oldValues,
                Map.of("name", role.getName(), "active", role.isActive()), servletRequest);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(UUID id,
                                          AssignPermissionsRequest request,
                                          HttpServletRequest servletRequest) {
        Role role = findAccessibleRole(id);
        enforceSystemRoleWrite(role);
        roleDelegationService.validateCanManageRole(roleDelegationService.currentUser(), role);
        List<Permission> permissions = permissionRepository.findAllById(request.permissionIds());
        Set<UUID> foundPermissionIds = new HashSet<>();
        permissions.forEach(permission -> foundPermissionIds.add(permission.getId()));
        request.permissionIds().stream()
                .filter(permissionId -> !foundPermissionIds.contains(permissionId))
                .findFirst()
                .ifPresent(permissionId -> {
                    throw new ResourceNotFoundException("Permission not found");
                });
        roleDelegationService.validateCanAssignPermissions(roleDelegationService.currentUser(), permissions);
        int actorLevel = roleDelegationService.getCurrentUserEffectiveLevel();
        Set<UUID> submittedPermissionIds = new HashSet<>(request.permissionIds());
        List<RolePermission> existingRolePermissions = rolePermissionRepository.findByRoleId(role.getId());
        existingRolePermissions.stream()
                .filter(rolePermission -> rolePermission.getPermission().getDelegationLevel() <= actorLevel)
                .filter(rolePermission -> !submittedPermissionIds.contains(rolePermission.getPermission().getId()))
                .forEach(rolePermission -> rolePermissionRepository.deleteByRoleIdAndPermissionId(
                        role.getId(),
                        rolePermission.getPermission().getId()
                ));
        permissions.forEach(permission -> {
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        });
        auditService.record(AuditActions.ROLE_UPDATED, role.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "Role", role.getId(), "Permissions assigned to role", null,
                Map.of("permissionIds", request.permissionIds().stream().map(UUID::toString).toList()), servletRequest);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse removePermission(UUID id, UUID permissionId, HttpServletRequest servletRequest) {
        Role role = findAccessibleRole(id);
        enforceSystemRoleWrite(role);
        roleDelegationService.validateCanManageRole(roleDelegationService.currentUser(), role);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        roleDelegationService.validateCanAssignPermission(roleDelegationService.currentUser(), permission);
        rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getId(), permission.getId());
        auditService.record(AuditActions.ROLE_UPDATED, role.getOrganizationId(), SecurityContextHelper.getCurrentUserId(),
                "Role", role.getId(), "Permission removed from role",
                Map.of("permissionId", permissionId.toString()), null, servletRequest);
        return toResponse(role);
    }

    private Role findAccessibleRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (!SecurityContextHelper.isSuperAdmin()
                && role.getOrganizationId() != null
                && !SecurityContextHelper.getCurrentOrganizationId().equals(role.getOrganizationId())) {
            throw new ForbiddenException("Cannot access role from another organization");
        }
        return role;
    }

    private UUID resolveRoleOrganizationId(CreateRoleRequest request) {
        if (SecurityContextHelper.isSuperAdmin()) {
            if (request.organizationId() != null && !organizationRepository.existsById(request.organizationId())) {
                throw new ResourceNotFoundException("Organization not found");
            }
            return request.systemRole() ? null : request.organizationId();
        }
        if (request.systemRole()) {
            throw new ForbiddenException("Only super admins may create system roles");
        }
        return SecurityContextHelper.getCurrentOrganizationId();
    }

    private void enforceSystemRoleWrite(Role role) {
        if ((role.isSystemRole() || role.getOrganizationId() == null) && !SecurityContextHelper.isSuperAdmin()) {
            throw new ForbiddenException("Only super admins may modify system roles");
        }
    }

    private boolean roleExists(String name, UUID organizationId) {
        if (organizationId == null) {
            return roleRepository.findByNameAndOrganizationIdIsNull(name).isPresent();
        }
        return roleRepository.findByNameAndOrganizationId(name, organizationId).isPresent();
    }

    private RoleResponse toResponse(Role role) {
        return roleMapper.toResponse(role, rolePermissionRepository.findPermissionCodesByRoleId(role.getId()));
    }

    private String normalizeName(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
