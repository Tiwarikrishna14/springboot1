package com.company.orderapproval.role.repository;

import com.company.orderapproval.role.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameAndOrganizationId(String name, UUID organizationId);

    Optional<Role> findByNameAndOrganizationIdIsNull(String name);

    @Query("""
            select r from Role r
            where (:includeAll = true
                   or r.organizationId is null
                   or r.organizationId = :organizationId)
              and (:search is null or :search = ''
                   or lower(r.name) like lower(concat('%', :search, '%'))
                   or lower(r.description) like lower(concat('%', :search, '%')))
            """)
    Page<Role> searchRoles(@Param("organizationId") UUID organizationId,
                           @Param("includeAll") boolean includeAll,
                           @Param("search") String search,
                           Pageable pageable);

    List<Role> findByNameIn(List<String> names);

    @Query("""
            select r from Role r
            where r.active = true
              and r.level < :actorLevel
              and (:includeAll = true
                   or r.organizationId is null
                   or r.organizationId = :organizationId)
            order by r.level desc, r.name
            """)
    List<Role> findAssignableRoles(@Param("organizationId") UUID organizationId,
                                   @Param("includeAll") boolean includeAll,
                                   @Param("actorLevel") int actorLevel);
}
