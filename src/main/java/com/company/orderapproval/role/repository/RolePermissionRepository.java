package com.company.orderapproval.role.repository;

import com.company.orderapproval.role.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    @Query("select rp from RolePermission rp where rp.role.id = :roleId")
    List<RolePermission> findByRoleId(@Param("roleId") UUID roleId);

    @Query("select rp from RolePermission rp where rp.role.id = :roleId and rp.permission.id = :permissionId")
    Optional<RolePermission> findByRoleIdAndPermissionId(@Param("roleId") UUID roleId,
                                                         @Param("permissionId") UUID permissionId);

    @Query("select count(rp) > 0 from RolePermission rp where rp.role.id = :roleId and rp.permission.id = :permissionId")
    boolean existsByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    @Modifying
    @Query("delete from RolePermission rp where rp.role.id = :roleId and rp.permission.id = :permissionId")
    void deleteByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    @Query("""
            select p.code from RolePermission rp
            join rp.permission p
            where rp.role.id = :roleId
            order by p.code
            """)
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);
}
