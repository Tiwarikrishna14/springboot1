package com.company.orderapproval.user.repository;

import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByBusinessCustomerId(UUID businessCustomerId);

    long countByBranchId(UUID branchId);

    long countByOrganizationId(UUID organizationId);

    @Modifying
    @Query("update User u set u.branchId = :branchId, u.updatedBy = :updatedBy, u.updatedAt = CURRENT_TIMESTAMP where u.businessCustomerId = :customerId")
    int transferBusinessCustomerUsers(@Param("customerId") UUID customerId,
                                      @Param("branchId") UUID branchId,
                                      @Param("updatedBy") UUID updatedBy);

    @Query("""
            select u from User u
            where (:organizationId is null or u.organizationId = :organizationId)
              and (:branchId is null or u.branchId = :branchId)
              and (:businessCustomerId is null or u.businessCustomerId = :businessCustomerId)
              and (:businessCustomerLocationId is null or u.businessCustomerLocationId = :businessCustomerLocationId)
              and (:status is null or u.status = :status)
              and (:status is not null or u.status <> :excludedStatus)
              and (:roleNamesEmpty = true or exists (
                    select ur.id
                    from UserRole ur
                    join ur.role r
                    where ur.user = u
                      and r.active = true
                      and upper(r.name) in :roleNames
              ))
              and (:search is null or :search = ''
                   or lower(u.firstName) like lower(concat('%', :search, '%'))
                   or lower(u.lastName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
            """)
    Page<User> searchUsers(@Param("organizationId") UUID organizationId,
                           @Param("branchId") UUID branchId,
                           @Param("businessCustomerId") UUID businessCustomerId,
                           @Param("businessCustomerLocationId") UUID businessCustomerLocationId,
                           @Param("status") UserStatus status,
                           @Param("excludedStatus") UserStatus excludedStatus,
                           @Param("roleNamesEmpty") boolean roleNamesEmpty,
                           @Param("roleNames") Collection<String> roleNames,
                           @Param("search") String search,
                           Pageable pageable);

    @Query("""
            select distinct u.id
            from User u
            join UserRole ur on ur.user = u
            join RolePermission rp on rp.role = ur.role
            join rp.permission p
            where u.id in :userIds
              and u.businessCustomerId = :businessCustomerId
              and u.status = :activeStatus
              and ur.role.active = true
              and p.code in :permissionCodes
            """)
    List<UUID> findEligibleApproverIds(@Param("userIds") Collection<UUID> userIds,
                                       @Param("businessCustomerId") UUID businessCustomerId,
                                       @Param("activeStatus") UserStatus activeStatus,
                                       @Param("permissionCodes") Collection<String> permissionCodes);

    @Query("""
            select distinct u
            from User u
            join UserRole ur on ur.user = u
            join RolePermission rp on rp.role = ur.role
            join rp.permission p
            where u.businessCustomerId = :businessCustomerId
              and u.status = :activeStatus
              and ur.role.active = true
              and p.code = :permissionCode
            order by u.firstName, u.lastName, u.email
            """)
    List<User> findApproverUsersByBusinessCustomerId(@Param("businessCustomerId") UUID businessCustomerId,
                                                     @Param("activeStatus") UserStatus activeStatus,
                                                     @Param("permissionCode") String permissionCode);
}
