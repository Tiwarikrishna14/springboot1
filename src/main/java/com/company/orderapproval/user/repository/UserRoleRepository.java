package com.company.orderapproval.user.repository;

import com.company.orderapproval.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("select ur from UserRole ur where ur.user.id = :userId")
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    @Query("select ur from UserRole ur where ur.user.id = :userId and ur.role.id = :roleId")
    Optional<UserRole> findByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Query("select count(ur) > 0 from UserRole ur where ur.user.id = :userId and ur.role.id = :roleId")
    boolean existsByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Modifying
    @Query("delete from UserRole ur where ur.user.id = :userId and ur.role.id = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Query("""
            select r.name from UserRole ur
            join ur.role r
            where ur.user.id = :userId
              and r.active = true
            order by r.name
            """)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    @Query("""
            select distinct p.code from UserRole ur
            join ur.role r
            join RolePermission rp on rp.role.id = r.id
            join rp.permission p
            where ur.user.id = :userId
              and r.active = true
            order by p.code
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);
}
