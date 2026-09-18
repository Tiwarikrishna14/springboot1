package com.company.orderapproval.branch.repository;
import com.company.orderapproval.branch.entity.Branch;
import com.company.orderapproval.branch.entity.BranchStatus;
import com.company.orderapproval.branch.dto.UpdateBranchRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Branch b
        SET b.name = COALESCE(:#{#request.name}, b.name),
            b.city = COALESCE(:#{#request.city}, b.city),
            b.address = COALESCE(:#{#request.address}, b.address),
            b.status = COALESCE(:#{#request.status}, b.status),
            b.updatedBy = :updatedBy,
            b.updatedAt = CURRENT_TIMESTAMP
        WHERE (
            :branchId IS NOT NULL
            AND b.id = :branchId
            AND (:organizationId IS NULL OR b.organizationId = :organizationId)
        ) OR (
            :branchId IS NULL
            AND b.organizationId = :organizationId
            AND UPPER(b.branchCode) = UPPER(:branchCode)
        )
        """)
    int updatePartial(@Param("branchId") UUID branchId,
                      @Param("organizationId") UUID organizationId,
                      @Param("branchCode") String branchCode,
                      @Param("request") UpdateBranchRequest request,
                      @Param("updatedBy") UUID updatedBy);

    @Query("""
    SELECT b.status
    FROM Branch b
    WHERE b.organizationId = :organizationId
    AND b.branchCode = :branchCode
    """)
    Optional<BranchStatus> findStatusByOrganizationIdAndBranchCode(
        @Param("organizationId") UUID organizationId,
        @Param("branchCode") String branchCode);
    Optional<Branch> findByOrganizationIdAndBranchCode(UUID organizationId, String branchCode);
    Page<Branch> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Branch> findByStatus(BranchStatus status, Pageable pageable);
    Page<Branch> findByOrganizationIdAndStatus(UUID organizationId, BranchStatus status, Pageable pageable);
    long countByOrganizationId(UUID organizationId);
}
