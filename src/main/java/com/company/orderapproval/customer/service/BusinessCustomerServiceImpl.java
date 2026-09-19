package com.company.orderapproval.customer.service;

import com.company.orderapproval.branch.entity.Branch;
import com.company.orderapproval.branch.entity.BranchStatus;
import com.company.orderapproval.branch.repository.BranchRepository;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.response.DeleteValidationResponse;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.dto.BusinessCustomerResponse;
import com.company.orderapproval.customer.dto.CreateBusinessCustomerRequest;
import com.company.orderapproval.customer.dto.UpdateBusinessCustomerRequest;
import com.company.orderapproval.customer.dto.TransferBusinessCustomerBranchRequest;
import com.company.orderapproval.customer.location.repository.BusinessCustomerLocationRepository;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.customer.entity.BusinessCustomerStatus;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.order.entity.OrderStatus;
import com.company.orderapproval.order.repository.OrderRepository;
import com.company.orderapproval.product.service.ProductRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class BusinessCustomerServiceImpl implements BusinessCustomerService {

    private static final Set<OrderStatus> NOT_DELIVERED_ORDER_STATUSES = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.CREATED,
            OrderStatus.CHANGES_REQUESTED,
            OrderStatus.APPROVED,
            OrderStatus.SUBMITTED,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED
    );

    private final BusinessCustomerRepository repo;
    private final BranchRepository branches;
    private final UserRepository users;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final BusinessCustomerLocationRepository locations;

    public BusinessCustomerServiceImpl(BusinessCustomerRepository repo,
                                       BranchRepository branches,
                                       UserRepository users,
                                       OrderRepository orders,
                                       ProductRepository products,
                                       BusinessCustomerLocationRepository locations) {
        this.repo = repo;
        this.branches = branches;
        this.users = users;
        this.orders = orders;
        this.products = products;
        this.locations = locations;
    }

    public Page<BusinessCustomerResponse> list(UUID organizationId,
                                               UUID branchId,
                                               String city,
                                               BusinessCustomerStatus status,
                                               String search,
                                               Pageable p) {
        if (status != null && status != BusinessCustomerStatus.ACTIVE) {
            return Page.empty(p);
        }
        BusinessCustomerStatus effectiveStatus = BusinessCustomerStatus.ACTIVE;
        UUID org = SecurityContextHelper.isSuperAdmin() ? organizationId : SecurityContextHelper.getCurrentOrganizationId();
        UUID scopedBranch = branchId;

        if (!SecurityContextHelper.isSuperAdmin() && !SecurityContextHelper.isOrganizationAdmin()) {
            UUID currentBranch = currentBranchId();
            if (currentBranch != null) {
                scopedBranch = currentBranch;
            }
            UUID customerId = currentBusinessCustomerId();
            if (customerId != null) {
                return repo.findById(customerId)
                        .filter(customer -> customer.getStatus() == effectiveStatus)
                        // .filter(customer -> search == null
                        //         || search.isBlank()
                        //         || customer.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                        .map(customer -> new PageImpl<BusinessCustomer>(List.of(customer), p, 1))
                        .orElse(new PageImpl<>(Collections.emptyList(), p, 0))
                        .map(this::response);
            }
        }

        return repo.search(org, scopedBranch, effectiveStatus, city, search, p).map(this::response);
    }

    public BusinessCustomerResponse get(UUID id) {
        BusinessCustomer customer = access(id);
        if (customer.getStatus() != BusinessCustomerStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer not found");
        }
        enforceBranchScopeIfPresent(customer);
        return response(customer);
    }

    @Transactional
    public BusinessCustomerResponse create(UUID branchId, CreateBusinessCustomerRequest request, HttpServletRequest http) {
        Branch branch = branch(branchId);
        enforceBranchScope(branch);

        String code = request.customerCode().trim().toUpperCase(Locale.ROOT);
        Optional<BusinessCustomerStatus> status = repo.findStatusByBranchIdAndCustomerCode(branchId, code);
    status.ifPresent(branchStatus -> {

    if (branchStatus == BusinessCustomerStatus.ACTIVE) {
        throw new ConflictException(
                "Customer code already exists in branch"
        );
    }

    if (branchStatus == BusinessCustomerStatus.INACTIVE) {
        throw new ConflictException(
                "Customer code already exists but is inactive. Do you want to reactivate it?", true);
    }
    });

        BusinessCustomer customer = new BusinessCustomer();
        customer.setOrganizationId(branch.getOrganizationId());
        customer.setBranchId(branchId);
        customer.setCustomerCode(code);
        customer.setName(request.name().trim());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setAddress(request.address());
        customer.setPincode(request.pincode());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCreatedBy(SecurityContextHelper.getCurrentUserId());
        customer.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        repo.save(customer);
        return response(customer);
    }

    @Transactional
    public BusinessCustomerResponse update(UUID id, UpdateBusinessCustomerRequest request, HttpServletRequest http) {
        BusinessCustomer customer = access(id);
        enforceBranchScopeIfPresent(customer);
        customer.setName(request.name().trim());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setAddress(request.address());
        customer.setPincode(request.pincode());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setStatus(request.status());
        customer.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        repo.save(customer);
        return response(customer);
    }

    @Transactional
    public BusinessCustomerResponse transferBranch(UUID id,
                                                   TransferBusinessCustomerBranchRequest request,
                                                   HttpServletRequest http) {
        if (!SecurityContextHelper.isSuperAdmin()
                && !SecurityContextHelper.hasRole("ORGANIZATION_ADMIN")) {
            throw new ForbiddenException("Only super admins or organization admins may transfer business customers");
        }

        BusinessCustomer customer = access(id);
        Branch targetBranch = branch(request.targetBranchId());
        if (targetBranch.getStatus() != BranchStatus.ACTIVE) {
            throw new BadRequestException("Target branch must be active");
        }
        if (!customer.getOrganizationId().equals(targetBranch.getOrganizationId())) {
            throw new BadRequestException("Business customer and target branch must belong to the same organization");
        }
        if (request.targetBranchId().equals(customer.getBranchId())) {
            throw new BadRequestException("Business customer is already assigned to the target branch");
        }
        if (repo.existsByBranchIdAndCustomerCodeAndIdNot(
                targetBranch.getId(), customer.getCustomerCode(), customer.getId())) {
            throw new ConflictException("Customer code already exists in target branch");
        }

        UUID actorId = SecurityContextHelper.getCurrentUserId();
        customer.setBranchId(targetBranch.getId());
        customer.setUpdatedBy(actorId);
        repo.save(customer);
        locations.transferBranch(customer.getId(), targetBranch.getId());
        users.transferBusinessCustomerUsers(customer.getId(), targetBranch.getId(), actorId);
        return response(customer);
    }

    public DeleteValidationResponse validateDelete(UUID id) {
        BusinessCustomer customer = access(id);
        enforceBranchScopeIfPresent(customer);
        Map<String, Long> counts = customerDeleteCounts(customer);
        List<String> warnings = customerDeleteWarnings(counts);
        return new DeleteValidationResponse(
                !warnings.isEmpty(),
                warnings.isEmpty()
                        ? "No business customer mappings found. Business customer can be deactivated safely."
                        : "Business customer has associated mappings. User confirmation is required before deactivation.",
                warnings,
                counts
        );
    }

    @Transactional
    public BusinessCustomerResponse delete(UUID id, boolean force, HttpServletRequest http) {
        BusinessCustomer customer = access(id);
        enforceBranchScopeIfPresent(customer);
        Map<String, Long> counts = customerDeleteCounts(customer);
        List<String> warnings = customerDeleteWarnings(counts);

        if (!warnings.isEmpty() && !force) {
            throw new BadRequestException(
                    "Cannot deactivate business customer without confirmation because "
                            + String.join(" and ", warnings)
                            + ". Call DELETE with force=true after user accepts the warning."
            );
        }

        customer.setStatus(BusinessCustomerStatus.INACTIVE);
        customer.setUpdatedBy(SecurityContextHelper.getCurrentUserId());
        repo.save(customer);
        return response(customer);
    }

    private Map<String, Long> customerDeleteCounts(BusinessCustomer customer) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", users.countByBusinessCustomerId(customer.getId()));
        counts.put("products", products.countByCustomerSellCodeIn(List.of(customer.getCustomerCode())));
        counts.put("notDeliveredOrders", orders.countByBusinessCustomerIdAndStatusIn(
                customer.getId(),
                NOT_DELIVERED_ORDER_STATUSES
        ));
        return counts;
    }

    private List<String> customerDeleteWarnings(Map<String, Long> counts) {
        List<String> warnings = new ArrayList<>();
        long userCount = counts.get("users");
        long productCount = counts.get("products");
        long orderCount = counts.get("notDeliveredOrders");

        if (userCount > 0) {
            warnings.add(userCount + " user(s) are mapped to this business customer");
        }
        if (productCount > 0) {
            warnings.add(productCount + " product(s) are mapped to this business customer");
        }
        if (orderCount > 0) {
            warnings.add(orderCount + " order(s) are not delivered for this business customer");
        }
        return warnings;
    }

    private Branch branch(UUID id) {
        Branch branch = branches.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!SecurityContextHelper.isSuperAdmin()
                && !branch.getOrganizationId().equals(SecurityContextHelper.getCurrentOrganizationId())) {
            throw new ForbiddenException("Cannot access another organization");
        }
        return branch;
    }

    private void enforceBranchScopeIfPresent(BusinessCustomer customer) {
        if (customer.getBranchId() != null) {
            enforceBranchScope(branch(customer.getBranchId()));
        }
    }

    private void enforceBranchScope(Branch branch) {
        if (!SecurityContextHelper.isSuperAdmin()) {
            UUID currentBranch = currentBranchId();
            if (currentBranch != null && !currentBranch.equals(branch.getId())) {
                throw new ForbiddenException("Cannot access another branch");
            }
        }
    }

    private BusinessCustomer access(UUID id) {
        BusinessCustomer customer = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
        if (!SecurityContextHelper.isSuperAdmin()
                && !customer.getOrganizationId().equals(SecurityContextHelper.getCurrentOrganizationId())) {
            throw new ForbiddenException("Cannot access another organization");
        }
        UUID currentCustomer = currentBusinessCustomerId();
        if (!SecurityContextHelper.isSuperAdmin()
                && currentCustomer != null
                && !currentCustomer.equals(id)) {
            throw new ForbiddenException("Cannot access another business customer");
        }
        return customer;
    }

    private UUID currentBranchId() {
        return SecurityContextHelper.getCurrentBranchId();
    }

    private UUID currentBusinessCustomerId() {
        return SecurityContextHelper.getCurrentBusinessCustomerId();
    }

    private BusinessCustomerResponse response(BusinessCustomer customer) {
        return new BusinessCustomerResponse(
                customer.getId(),
                customer.getOrganizationId(),
                customer.getBranchId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getCity(),
                customer.getState(),
                customer.getAddress(),
                customer.getPincode(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
