package com.company.orderapproval.policy.repository;

import com.company.orderapproval.policy.entity.ApprovalPolicy;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, UUID> {
    Optional<ApprovalPolicy> findByScopeTypeAndScopeIdAndPolicyTypeAndActiveTrue(
            String scopeType, UUID scopeId, String policyType);

    @Modifying
    @Query(value = """
        INSERT INTO approval_policies
            (scope_type, scope_id, policy_type, configuration, active, created_by, updated_by)
        VALUES (:scopeType, :scopeId, :policyType, CAST(:configuration AS jsonb), TRUE, :actorId, :actorId)
        ON CONFLICT (scope_type, scope_id, policy_type)
        DO UPDATE SET configuration = EXCLUDED.configuration,
                      active = TRUE,
                      updated_by = EXCLUDED.updated_by,
                      updated_at = now()
        """, nativeQuery = true)
    int upsert(@Param("scopeType") String scopeType,
               @Param("scopeId") UUID scopeId,
               @Param("policyType") String policyType,
               @Param("configuration") String configuration,
               @Param("actorId") UUID actorId);
}

