package com.company.orderapproval.branch.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.branch.dto.*;
import com.company.orderapproval.branch.entity.*;
import com.company.orderapproval.branch.repository.BranchRepository;
import com.company.orderapproval.common.exception.*;
import com.company.orderapproval.common.response.DeleteValidationResponse;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.order.entity.OrderStatus;
import com.company.orderapproval.order.repository.OrderRepository;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.product.service.ProductCustomerMappingRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BranchServiceImpl implements BranchService {

 private static final Set<OrderStatus> NOT_DELIVERED_ORDER_STATUSES = Set.of(
         OrderStatus.DRAFT,
         OrderStatus.CREATED,
         OrderStatus.CHANGES_REQUESTED,
         OrderStatus.APPROVED,
         OrderStatus.SUBMITTED,
         OrderStatus.PENDING,
         OrderStatus.CONFIRMED
 );

 private final BranchRepository repo;
 private final OrganizationRepository orgs;
 private final AuditService audit;
 private final UserRepository users;
 private final BusinessCustomerRepository customers;
 private final ProductCustomerMappingRepository products;
 private final OrderRepository orders;

 public BranchServiceImpl(
         BranchRepository repo,
         OrganizationRepository orgs,
         AuditService audit,
         UserRepository users,
         BusinessCustomerRepository customers,
         ProductCustomerMappingRepository products,
         OrderRepository orders
 ) {
  this.repo = repo;
  this.orgs = orgs;
  this.audit = audit;
  this.users = users;
  this.customers = customers;
  this.products = products;
  this.orders = orders;
 }

 public Page<BranchResponse> list(
         UUID organizationId,
         Pageable p
 ) {
  UUID org = organizationId;

  if (!SecurityContextHelper.isSuperAdmin()) {
   org = SecurityContextHelper.getCurrentOrganizationId();

   UUID branchId = currentBranch();

   if (branchId != null && !hasOrganizationWideBranchAccess()) {
    return repo.findById(branchId)
            .filter(b -> b.getStatus() == BranchStatus.ACTIVE)
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
          ? repo.findByStatus(BranchStatus.ACTIVE, p)
          : repo.findByOrganizationIdAndStatus(org, BranchStatus.ACTIVE, p)
  ).map(this::response);
 }

 public BranchResponse get(UUID id) {
  Branch branch = access(id);
  if (branch.getStatus() != BranchStatus.ACTIVE) {
   throw new ResourceNotFoundException("Branch not found");
  }
  return response(branch);
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
        Optional<BranchStatus> status = repo.findStatusByOrganizationIdAndBranchCode(organizationId, code);
  status.ifPresent(branchStatus -> {

    if (branchStatus == BranchStatus.ACTIVE) {
        throw new ConflictException(
                "Branch code already exists"
        );
    }

    if (branchStatus == BranchStatus.INACTIVE) {
        throw new ConflictException(
                "Branch already exists but is inactive. Do you want to reactivate it?", true);
    }
});

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
 public void update(
         UUID id,
         UUID organizationId,
         String branchCode,
         UpdateBranchRequest r,
         HttpServletRequest h
 ) {
  if (r.name() == null && r.city() == null && r.address() == null && r.status() == null) {
   throw new BadRequestException("Provide at least one field to update");
  }
  if (r.name() != null) {
   if (r.name().isBlank()) {
    throw new BadRequestException("Branch name cannot be blank");
   }
  }
  if (organizationId == null || branchCode == null || branchCode.isBlank()) {
   if (id == null) {
    throw new BadRequestException("Provide branch id, or provide both organizationId and branchCode");
   }
  }

  UUID effectiveOrganizationId = SecurityContextHelper.isSuperAdmin()
          ? organizationId
          : SecurityContextHelper.getCurrentOrganizationId();
  if (!hasOrganizationWideBranchAccess()) {
   UUID scopedBranchId = currentBranch();
   if (scopedBranchId != null && id != null && !scopedBranchId.equals(id)) {
    throw new ForbiddenException("Cannot update another branch");
   }
   if (scopedBranchId != null) {
    id = scopedBranchId;
   }
  }
  String normalizedBranchCode = branchCode == null
          ? null
          : branchCode.trim().toUpperCase(Locale.ROOT);

  int updated = repo.updatePartial(
          id,
          effectiveOrganizationId,
          normalizedBranchCode,
          r,
          SecurityContextHelper.getCurrentUserId()
  );
  if (updated == 0) {
   throw new ResourceNotFoundException("Branch not found or cannot be updated in the current organization");
  }

 }

 public DeleteValidationResponse validateDelete(UUID id) {
  Branch b = access(id);
  Map<String, Long> counts = branchDeleteCounts(b.getId());
  List<String> warnings = branchDeleteWarnings(counts);
  return new DeleteValidationResponse(
          !warnings.isEmpty(),
          warnings.isEmpty()
                  ? "No branch mappings found. Branch can be deactivated safely."
                  : "Branch has associated mappings. User confirmation is required before deactivation.",
          warnings,
          counts
  );
 }

 @Transactional
 public BranchResponse delete(
         UUID id,
         boolean force,
         HttpServletRequest h
 ) {
  Branch b = access(id);
  Map<String, Long> counts = branchDeleteCounts(b.getId());
  List<String> warnings = branchDeleteWarnings(counts);

  if (!warnings.isEmpty() && !force) {
   throw new BadRequestException(
           "Cannot deactivate branch without confirmation because "
                   + String.join(" and ", warnings)
                   + ". Call DELETE with force=true after user accepts the warning."
   );
  }

  if (counts.get("businessCustomers") > 0) {
   customers.detachBranchMapping(
           b.getId(),
           SecurityContextHelper.getCurrentUserId()
   );
  }

  b.setStatus(BranchStatus.INACTIVE);
  b.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
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

  UUID current = hasOrganizationWideBranchAccess() ? null : currentBranch();

  if (current != null && !current.equals(id)) {
   throw new ForbiddenException(
           "Cannot access another branch"
   );
  }

  return b;
 }

 private Map<String, Long> branchDeleteCounts(UUID branchId) {
  List<String> customerCodes = customers.findCustomerCodesByBranchId(branchId);
  long productCount = customerCodes.isEmpty()
          ? 0
          : products.countByBusinessCustomerCustomerCodeIn(customerCodes);

  Map<String, Long> counts = new LinkedHashMap<>();
  counts.put("users", users.countByBranchId(branchId));
  counts.put("businessCustomers", customers.countByBranchId(branchId));
  counts.put("products", productCount);
  counts.put("notDeliveredOrders", orders.countByBranchIdAndStatusIn(branchId, NOT_DELIVERED_ORDER_STATUSES));
  return counts;
 }

 private List<String> branchDeleteWarnings(Map<String, Long> counts) {
  List<String> warnings = new ArrayList<>();
  long userCount = counts.get("users");
  long customerCount = counts.get("businessCustomers");
  long productCount = counts.get("products");
  long orderCount = counts.get("notDeliveredOrders");

  if (userCount > 0) {
   warnings.add(userCount + " user(s) are mapped to this branch");
  }
  if (customerCount > 0) {
   warnings.add(customerCount + " business customer(s) are mapped to this branch and will be moved to organization level");
  }
  if (productCount > 0) {
   warnings.add(productCount + " product(s) are mapped through this branch's business customers");
  }
  if (orderCount > 0) {
   warnings.add(orderCount + " order(s) are not delivered for this branch");
  }
  return warnings;
 }

 private UUID currentBranch() {
  return users.findById(
          SecurityContextHelper.getCurrentUserId()
  ).map(User::getBranchId).orElse(null);
 }

 private boolean hasOrganizationWideBranchAccess() {
  return SecurityContextHelper.isSuperAdmin()
          || SecurityContextHelper.hasRole("ORGANIZATION_ADMIN");
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
