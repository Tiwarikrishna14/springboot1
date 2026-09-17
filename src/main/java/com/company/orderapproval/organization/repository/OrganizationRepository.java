package com.company.orderapproval.organization.repository;

import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.entity.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByOrganizationCode(String organizationCode);

    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);
}
