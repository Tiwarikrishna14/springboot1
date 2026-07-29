package com.company.orderapproval.user.repository;

import com.company.orderapproval.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (:organizationId is null or u.organizationId = :organizationId)
              and (:branchId is null or u.branchId = :branchId)
              and (:businessCustomerId is null or u.businessCustomerId = :businessCustomerId)
              and (:businessCustomerLocationId is null or u.businessCustomerLocationId = :businessCustomerLocationId)
              and (:search is null or :search = ''
                   or lower(u.firstName) like lower(concat('%', :search, '%'))
                   or lower(u.lastName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
            """)
    Page<User> searchUsers(@Param("organizationId") UUID organizationId,
                           @Param("branchId") UUID branchId,
                           @Param("businessCustomerId") UUID businessCustomerId,
                           @Param("businessCustomerLocationId") UUID businessCustomerLocationId,
                           @Param("search") String search,
                           Pageable pageable);
}
