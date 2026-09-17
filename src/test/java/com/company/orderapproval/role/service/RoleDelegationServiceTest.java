package com.company.orderapproval.role.service;

import com.company.orderapproval.permission.entity.Permission;
import com.company.orderapproval.permission.repository.PermissionRepository;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleDelegationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final RoleDelegationService service = new RoleDelegationService(
            userRepository,
            userRoleRepository,
            roleRepository,
            permissionRepository
    );

    @Test
    void usesHighestAssignedRoleLevel() {
        User user = user();
        when(userRoleRepository.findMaxRoleLevelByUserId(user.getId())).thenReturn(80);

        assertThat(service.getEffectiveLevel(user)).isEqualTo(80);
    }

    @Test
    void allowsAssigningOnlyLowerRoles() {
        User actor = user();
        when(userRoleRepository.findMaxRoleLevelByUserId(actor.getId())).thenReturn(80);

        assertThat(service.canAssignRole(actor, role(60))).isTrue();
        assertThat(service.canAssignRole(actor, role(80))).isFalse();
        assertThat(service.canAssignRole(actor, role(100))).isFalse();
    }

    @Test
    void rejectsPermissionAboveActorLevel() {
        User actor = user();
        when(userRoleRepository.findMaxRoleLevelByUserId(actor.getId())).thenReturn(80);

        service.validateCanAssignPermission(actor, permission(80));
        assertThatThrownBy(() -> service.validateCanAssignPermission(actor, permission(100)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(UUID.randomUUID());
        return user;
    }

    private Role role(int level) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setLevel(level);
        role.setActive(true);
        return role;
    }

    private Permission permission(int delegationLevel) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setDelegationLevel(delegationLevel);
        return permission;
    }
}
