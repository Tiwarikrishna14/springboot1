package com.company.orderapproval.branch.repository;
import com.company.orderapproval.branch.entity.Branch;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findByOrganizationIdAndBranchCode(UUID organizationId, String branchCode);
    Page<Branch> findByOrganizationId(UUID organizationId, Pageable pageable);
}
