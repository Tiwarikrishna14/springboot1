package com.company.orderapproval.branch.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.branch.dto.*;
import com.company.orderapproval.branch.entity.*;
import com.company.orderapproval.branch.repository.BranchRepository;
import com.company.orderapproval.common.exception.*;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BranchServiceImpl implements BranchService {

 private final BranchRepository repo;
 private final OrganizationRepository orgs;
 private final AuditService audit;
 private final UserRepository users;

 public BranchServiceImpl(
         BranchRepository repo,
         OrganizationRepository orgs,
         AuditService audit,
         UserRepository users
 ) {
  this.repo = repo;
  this.orgs = orgs;
  this.audit = audit;
  this.users = users;
 }

 public Page<BranchResponse> list(
         UUID organizationId,
         Pageable p
 ) {
  UUID org = organizationId;

  if (!SecurityContextHelper.isSuperAdmin()) {
   org = SecurityContextHelper.getCurrentOrganizationId();

   UUID branchId = currentBranch();

   if (branchId != null) {
    return repo.findById(branchId)
            .map(b -> new PageImpl<>(
                    List.of(response(b)),
                    p,
                    1
            ))
            .orElse(
                    new PageImpl<>(
                            List.of(),
                            p,
                            0
                    )
            );
   }
  }

  return (org == null
          ? repo.findAll(p)
          : repo.findByOrganizationId(org, p)
  ).map(this::response);
 }

 public BranchResponse get(UUID id) {
  return response(access(id));
 }

 @Transactional
 public BranchResponse create(
         UUID organizationId,
         CreateBranchRequest r,
         HttpServletRequest h
 ) {
  if (!SecurityContextHelper.isSuperAdmin()
          && !SecurityContextHelper.hasRole("ORGANIZATION_ADMIN")) {

   throw new ForbiddenException(
           "Only super admins or organization admins may create branches"
   );
  }

  if (!SecurityContextHelper.isSuperAdmin()
          && !SecurityContextHelper
          .getCurrentOrganizationId()
          .equals(organizationId)) {

   throw new ForbiddenException(
           "Cannot create a branch for another organization"
   );
  }

  if (!orgs.existsById(organizationId)) {
   throw new ResourceNotFoundException(
           "Organization not found"
   );
  }

  String code = r.branchCode()
          .trim()
          .toUpperCase(Locale.ROOT);

  if (repo.findByOrganizationIdAndBranchCode(
          organizationId,
          code
  ).isPresent()) {

   throw new ConflictException(
           "Branch code already exists"
   );
  }

  Branch b = new Branch();

  b.setOrganizationId(organizationId);
  b.setBranchCode(code);
  b.setName(r.name().trim());
  b.setCity(r.city());
  b.setAddress(r.address());

  b.setCreatedBy(
          SecurityContextHelper.getCurrentUserId()
  );

  b.setUpdatedBy(
          SecurityContextHelper.getCurrentUserId()
  );

  repo.save(b);

  return response(b);
 }

 @Transactional
 public BranchResponse update(
         UUID id,
         UpdateBranchRequest r,
         HttpServletRequest h
 ) {
  Branch b = access(id);

  b.setName(r.name().trim());
  b.setCity(r.city());
  b.setAddress(r.address());
  b.setStatus(r.status());

  b.setUpdatedBy(
          SecurityContextHelper.getCurrentUserId()
  );

  repo.save(b);

  return response(b);
 }

 private Branch access(UUID id) {
  Branch b = repo.findById(id)
          .orElseThrow(
                  () -> new ResourceNotFoundException(
                          "Branch not found"
                  )
          );

  if (!SecurityContextHelper.isSuperAdmin()
          && !b.getOrganizationId().equals(
          SecurityContextHelper.getCurrentOrganizationId()
  )) {

   throw new ForbiddenException(
           "Cannot access branch from another organization"
   );
  }

  UUID current = currentBranch();

  if (current != null && !current.equals(id)) {
   throw new ForbiddenException(
           "Cannot access another branch"
   );
  }

  return b;
 }

 private UUID currentBranch() {
  return users.findById(
          SecurityContextHelper.getCurrentUserId()
  ).map(User::getBranchId).orElse(null);
 }

 private BranchResponse response(Branch b) {
  return new BranchResponse(
          b.getId(),
          b.getOrganizationId(),
          b.getBranchCode(),
          b.getName(),
          b.getCity(),
          b.getAddress(),
          b.getStatus(),
          b.getCreatedAt(),
          b.getUpdatedAt()
  );
 }
}