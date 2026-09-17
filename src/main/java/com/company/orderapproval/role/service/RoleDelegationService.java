package com.company.orderapproval.role.service;

import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.permission.entity.Permission;
import com.company.orderapproval.permission.repository.PermissionRepository;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RoleDelegationService {

    private static final int MIN_CUSTOM_ROLE_LEVEL = 1;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleDelegationService(UserRepository userRepository,
                                 UserRoleRepository userRoleRepository,
                                 RoleRepository roleRepository,
                                 PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public User currentUser() {
        return userRepository.findById(SecurityContextHelper.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public int getEffectiveLevel(User user) {
        return userRoleRepository.findMaxRoleLevelByUserId(user.getId());
    }

    public int getCurrentUserEffectiveLevel() {
        return getEffectiveLevel(currentUser());
    }

    public boolean canAssignRole(User actor, Role targetRole) {
        Integer targetLevel = targetRole.getLevel();
        return targetLevel != null && targetLevel < getEffectiveLevel(actor);
    }

    public void validateCanAssignRole(User actor, Role targetRole) {
        if (!canAssignRole(actor, targetRole)) {
            throw new AccessDeniedException("Cannot assign a role at your level or above");
        }
    }

    public void validateCanManageRole(User actor, Role targetRole) {
        if (!canAssignRole(actor, targetRole)) {
            throw new AccessDeniedException("Cannot manage a role at your level or above");
        }
    }

    public void validateRoleLevelForWrite(User actor, Integer requestedLevel) {
        int actorLevel = getEffectiveLevel(actor);
        int level = requestedLevel == null ? actorLevel - 1 : requestedLevel;
        if (level < MIN_CUSTOM_ROLE_LEVEL || level >= actorLevel) {
            throw new AccessDeniedException("Role level must be lower than your highest role level");
        }
    }

    public int resolveRoleLevelForCreate(User actor, Integer requestedLevel) {
        int actorLevel = getEffectiveLevel(actor);
        int level = requestedLevel == null ? actorLevel - 1 : requestedLevel;
        validateRoleLevelForWrite(actor, level);
        return level;
    }

    public boolean canAssignPermission(User actor, Permission permission) {
        Integer delegationLevel = permission.getDelegationLevel();
        return delegationLevel != null && delegationLevel <= getEffectiveLevel(actor);
    }

    public void validateCanAssignPermission(User actor, Permission permission) {
        if (!canAssignPermission(actor, permission)) {
            throw new AccessDeniedException("Cannot delegate permission above your level");
        }
    }

    public void validateCanAssignPermissions(User actor, Collection<Permission> permissions) {
        permissions.forEach(permission -> validateCanAssignPermission(actor, permission));
    }

    public void validateTargetUserScope(User actor, User target) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return;
        }
        if (!Objects.equals(actor.getOrganizationId(), target.getOrganizationId())) {
            throw new AccessDeniedException("Cannot manage user from another organization");
        }
        if (actor.getBranchId() != null && !Objects.equals(actor.getBranchId(), target.getBranchId())) {
            throw new AccessDeniedException("Cannot manage user from another branch");
        }
        if (actor.getBusinessCustomerId() != null
                && !Objects.equals(actor.getBusinessCustomerId(), target.getBusinessCustomerId())) {
            throw new AccessDeniedException("Cannot manage user from another business customer");
        }
        if (actor.getBusinessCustomerLocationId() != null
                && !Objects.equals(actor.getBusinessCustomerLocationId(), target.getBusinessCustomerLocationId())) {
            throw new AccessDeniedException("Cannot manage user from another business customer location");
        }
    }

    public List<Role> getAssignableRoles(User actor) {
        boolean includeAll = SecurityContextHelper.isSuperAdmin();
        UUID organizationId = includeAll ? null : actor.getOrganizationId();
        return roleRepository.findAssignableRoles(organizationId, includeAll, getEffectiveLevel(actor));
    }

    public List<Permission> getAssignablePermissions(User actor) {
        return permissionRepository.findByDelegationLevelLessThanEqualOrderByModuleAscCodeAsc(getEffectiveLevel(actor));
    }
}
