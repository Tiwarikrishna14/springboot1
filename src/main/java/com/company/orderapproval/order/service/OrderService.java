package com.company.orderapproval.order.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.exception.ValidationException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.customer.entity.BusinessCustomerStatus;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocation;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus;
import com.company.orderapproval.customer.location.repository.BusinessCustomerLocationRepository;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.order.dto.ApprovalAction;
import com.company.orderapproval.order.dto.ApprovalActionRequest;
import com.company.orderapproval.order.dto.CreateOrderRequest;
import com.company.orderapproval.order.dto.OrderApproverResponse;
import com.company.orderapproval.order.dto.OrderApproverRequest;
import com.company.orderapproval.order.dto.OrderItemRequest;
import com.company.orderapproval.order.dto.OrderItemResponse;
import com.company.orderapproval.order.dto.OrderLocationResponse;
import com.company.orderapproval.order.dto.OrderResponse;
import com.company.orderapproval.order.dto.SupplierAction;
import com.company.orderapproval.order.dto.SupplierActionRequest;
import com.company.orderapproval.order.dto.UpdateOrderRequest;
import com.company.orderapproval.order.entity.ApprovalStatus;
import com.company.orderapproval.order.entity.Order;
import com.company.orderapproval.order.entity.OrderApprover;
import com.company.orderapproval.order.entity.OrderItem;
import com.company.orderapproval.order.entity.OrderNumberCounter;
import com.company.orderapproval.order.entity.OrderStatus;
import com.company.orderapproval.order.event.OrderEvent;
import com.company.orderapproval.order.repository.OrderNumberCounterRepository;
import com.company.orderapproval.order.repository.OrderRepository;
import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.entity.OrganizationType;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.product.entity.Product;
import com.company.orderapproval.product.service.ProductRepository;
import com.company.orderapproval.product.service.ProductCustomerMappingRepository;
import com.company.orderapproval.product.entity.ProductCustomerMapping;
import com.company.orderapproval.policy.dto.*;
import com.company.orderapproval.policy.service.ApprovalPolicyService;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.entity.UserStatus;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_APPROVE = "ORDER_APPROVE";
    private static final String ORDER_REJECT = "ORDER_REJECT";
    private static final String ORDER_UPDATE = "ORDER_UPDATE";
    private static final String ORGANIZATION_ADMIN = "ORGANIZATION_ADMIN";
    private static final String BRANCH_ADMIN = "BRANCH_ADMIN";
    private static final Set<OrderStatus> EDITABLE_STATUSES = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.CREATED,
            OrderStatus.CHANGES_REQUESTED
    );
    private static final Set<OrderStatus> SUPPLIER_VISIBLE_STATUSES = Set.of(
            OrderStatus.SUBMITTED,
            OrderStatus.REJECTED,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final OrderNumberCounterRepository orderNumberCounterRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final BusinessCustomerRepository businessCustomerRepository;
    private final BusinessCustomerLocationRepository businessCustomerLocationRepository;
    private final ProductRepository productRepository;
    private final ProductCustomerMappingRepository productCustomerMappingRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalPolicyService approvalPolicyService;

    @Transactional
    public OrderResponse create(CreateOrderRequest request, HttpServletRequest servletRequest) {
        User creator = currentUser();
        BusinessCustomer customer = creatorBusinessCustomer(creator);

        Order order = new Order();
        order.setOrderNumber(nextOrderNumber(customer));
        order.setOrganizationId(customer.getOrganizationId());
        order.setBranchId(customer.getBranchId());
        order.setBusinessCustomerId(customer.getId());
        order.setBusinessCustomerLocationId(creator.getBusinessCustomerLocationId());
        order.setBusinessCustomerCode(customer.getCustomerCode());
        order.setBusinessCustomerName(customer.getName());
        OrderApprovalPolicyConfig approvalPolicy = approvalPolicyService.resolveOrderPolicy(customer.getId());
        order.setApprovalPolicySnapshot(approvalPolicy);
        order.setCreatedBy(creator.getId());
        applyFields(order, request.notes(), request.remarks(), request.priority(), request.location(), request.referenceNumber());
        applyOrderLocation(order, creator, customer, request.businessCustomerLocationId(), request.locationCode(), request.location(), true);

        syncItems(order, request.products(), customer);
        syncApprovers(order, requestedApprovers(request.approvers(), request.approverIds()),
                customer.getId(), creator.getId(), approvalPolicy);
        order.setStatus(orderIsComplete(order) ? OrderStatus.CREATED : OrderStatus.DRAFT);

        Order saved = orderRepository.save(order);
        audit(saved, AuditActions.ORDER_CREATED, "Order created", servletRequest);
        publish(AuditActions.ORDER_CREATED, saved, creator.getId());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse update(UUID orderId, UpdateOrderRequest request, HttpServletRequest servletRequest) {
        Order order = findDetailedForUpdate(orderId);
        User actor = currentUser();
        assertEditable(order);
        assertCustomerSideMutationAllowed(order, actor);

        BusinessCustomer customer = businessCustomer(order.getBusinessCustomerId());
        boolean hadProducts = !order.getItems().isEmpty();
        boolean productsExplicitlyRemoved = request.products() != null && request.products().isEmpty() && hadProducts;

        applyFields(order, request.notes(), request.remarks(), request.priority(), request.location(), request.referenceNumber());
        applyOrderLocation(order, actor, customer, request.businessCustomerLocationId(), request.locationCode(), request.location(), false);
        syncItems(order, request.products(), customer);
        OrderApprovalPolicyConfig approvalPolicy = policySnapshot(order);
        syncApprovers(order, requestedApprovers(request.approvers(), request.approverIds()),
                customer.getId(), order.getCreatedBy(), approvalPolicy);

        if (productsExplicitlyRemoved) {
            order.setStatus(OrderStatus.ABANDONED);
        } else {
            order.setStatus(orderIsComplete(order) ? OrderStatus.CREATED : OrderStatus.DRAFT);
            resetApprovalActivity(order);
        }

        Order saved = orderRepository.save(order);
        audit(saved, AuditActions.ORDER_UPDATED, "Order updated", servletRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertVisible(order, currentUser());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(
            OrderStatus status,
            UUID createdBy,
            UUID businessCustomerId,
            String orderNumber,
            Pageable pageable
    ) {
        User user = currentUser();
        Specification<Order> specification = visibleTo(user)
                .and(matchesStatus(status))
                .and(matchesCreatedBy(createdBy))
                .and(matchesBusinessCustomer(businessCustomerId))
                .and(matchesOrderNumber(orderNumber));
        return orderRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse actOnApproval(UUID orderId, ApprovalActionRequest request, HttpServletRequest servletRequest) {
        Order order = findDetailedForUpdate(orderId);
        User actor = currentUser();
        assertVisible(order, actor);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Order is not in an approval state");
        }

        OrderApprover approver = order.getApprovers().stream()
                .filter(candidate -> candidate.getUserId().equals(actor.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You are not assigned as an approver for this order"));
        assertEligibleApprover(actor.getId(), order.getBusinessCustomerId(), approvalPermission(request.action()));
        assertApproverCanAct(order, approver);

        ApprovalStatus approvalStatus = switch (request.action()) {
            case APPROVE -> ApprovalStatus.APPROVED;
            case REQUEST_CHANGES -> ApprovalStatus.CHANGES_REQUESTED;
            case REJECT -> ApprovalStatus.REJECTED;
        };
        approver.setApprovalStatus(approvalStatus);
        approver.setRemark(cleanOptional(request.remark()));
        approver.setActedAt(Instant.now());

        String auditAction = AuditActions.ORDER_UPDATED;
        if (request.action() == ApprovalAction.REQUEST_CHANGES) {
            order.setStatus(OrderStatus.CHANGES_REQUESTED);
            auditAction = AuditActions.ORDER_CHANGES_REQUESTED;
        } else if (request.action() == ApprovalAction.REJECT) {
            order.setStatus(OrderStatus.REJECTED);
            auditAction = AuditActions.ORDER_REJECTED;
        } else if (approvalRequirementsSatisfied(order)) {
            order.setStatus(OrderStatus.APPROVED);
            auditAction = AuditActions.ORDER_APPROVED;
        }

        Order saved = orderRepository.save(order);
        audit(saved, auditAction, "Order approval action recorded", servletRequest);
        if (auditAction.equals(AuditActions.ORDER_CHANGES_REQUESTED) || auditAction.equals(AuditActions.ORDER_APPROVED)) {
            publish(auditAction, saved, actor.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse submit(UUID orderId, HttpServletRequest servletRequest) {
        Order order = findDetailedForUpdate(orderId);
        User actor = currentUser();
        assertVisible(order, actor);
        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new BadRequestException("Only approved orders can be submitted");
        }
        if (!actor.getId().equals(order.getCreatedBy()) && !isAssignedEligibleApprover(order, actor.getId())) {
            throw new ForbiddenException("Only the creator or an assigned authorized approver can submit this order");
        }

        order.setStatus(OrderStatus.SUBMITTED);
        Order saved = orderRepository.save(order);
        audit(saved, AuditActions.ORDER_SUBMITTED, "Order submitted", servletRequest);
        publish(AuditActions.ORDER_SUBMITTED, saved, actor.getId());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse supplierAction(UUID orderId, SupplierActionRequest request, HttpServletRequest servletRequest) {
        Order order = findDetailedForUpdate(orderId);
        User actor = currentUser();
        assertSupplierActionAllowed(actor, request.action());
        assertVisible(order, actor);

        String auditAction;
        switch (request.action()) {
            case REJECT -> {
                requireStatus(order, OrderStatus.SUBMITTED, "Only submitted orders can be rejected by supplier");
                order.setStatus(OrderStatus.REJECTED);
                auditAction = AuditActions.ORDER_REJECTED;
            }
            case PENDING -> {
                requireStatus(order, OrderStatus.SUBMITTED, "Only submitted orders can be marked pending");
                order.setExpectedDeliveryDate(validExpectedDeliveryDate(request.expectedDeliveryDate()));
                order.setStatus(OrderStatus.PENDING);
                auditAction = AuditActions.ORDER_PENDING;
            }
            case CONFIRM -> {
                if (order.getStatus() != OrderStatus.SUBMITTED && order.getStatus() != OrderStatus.PENDING) {
                    throw new BadRequestException("Only submitted or pending orders can be confirmed");
                }
                order.setExpectedDeliveryDate(validExpectedDeliveryDate(request.expectedDeliveryDate()));
                order.setStatus(OrderStatus.CONFIRMED);
                auditAction = AuditActions.ORDER_CONFIRMED;
            }
            case DELIVER -> {
                requireStatus(order, OrderStatus.CONFIRMED, "Only confirmed orders can be delivered");
                order.setStatus(OrderStatus.DELIVERED);
                auditAction = AuditActions.ORDER_DELIVERED;
            }
            default -> throw new BadRequestException("Unsupported supplier action");
        }

        String remark = cleanOptional(request.remark());
        if (remark != null) {
            order.setRemarks(remark);
        }

        Order saved = orderRepository.save(order);
        audit(saved, auditAction, "Supplier order action recorded", servletRequest);
        if (auditAction.equals(AuditActions.ORDER_CONFIRMED) || auditAction.equals(AuditActions.ORDER_DELIVERED)) {
            publish(auditAction, saved, actor.getId());
        }
        return toResponse(saved);
    }

    private String nextOrderNumber(BusinessCustomer customer) {
        String companyCode = companyCode(customer);
        orderNumberCounterRepository.insertIfMissing(customer.getId(), companyCode);
        OrderNumberCounter counter = orderNumberCounterRepository.findByBusinessCustomerIdForUpdate(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order number counter not found"));
        long sequence = counter.getNextSequence();
        counter.setNextSequence(sequence + 1);
        return counter.getCompanyCode() + "-SO-" + String.format(Locale.ROOT, "%03d", sequence);
    }

    private void syncItems(Order order, List<OrderItemRequest> requestedItems, BusinessCustomer customer) {
        if (requestedItems == null) {
            return;
        }
        if (requestedItems.isEmpty()) {
            order.getItems().clear();
            return;
        }

        List<Long> requestedProductIds = distinctProductIds(requestedItems);
        Map<Long, Product> productsById = new HashMap<>();
        productRepository.findAllById(requestedProductIds).forEach(product -> productsById.put(product.getId(), product));

        List<Long> missingProductIds = requestedProductIds.stream()
                .filter(productId -> !productsById.containsKey(productId))
                .toList();
        if (!missingProductIds.isEmpty()) {
            throw new ValidationException("Invalid products", Map.of("productIds", missingProductIds.toString()));
        }

        Map<Long, ProductCustomerMapping> mappingsByProductId = productCustomerMappingRepository
                .findByProductIdIn(requestedProductIds)
                .stream()
                .filter(mapping -> mapping.getBusinessCustomer().getId().equals(customer.getId()))
                .collect(Collectors.toMap(mapping -> mapping.getProduct().getId(), Function.identity()));
        List<Long> invalidCustomerProducts = requestedProductIds.stream()
                .filter(productId -> !mappingsByProductId.containsKey(productId))
                .toList();
        if (!invalidCustomerProducts.isEmpty()) {
            throw new ValidationException("Invalid products", Map.of("productIds", invalidCustomerProducts + " do not belong to customer " + customer.getCustomerCode()));
        }

        List<Long> inactiveProductIds = productsById.values().stream()
                .filter(product -> !"ACTIVE".equalsIgnoreCase(product.getStatus())
                        || !"ACTIVE".equalsIgnoreCase(mappingsByProductId.get(product.getId()).getStatus()))
                .map(Product::getId)
                .toList();
        if (!inactiveProductIds.isEmpty()) {
            throw new ValidationException("Invalid products", Map.of("productIds", inactiveProductIds + " are inactive"));
        }

        Map<Long, OrderItem> existingItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getProductId, Function.identity()));
        Set<Long> requestedProductIdSet = new HashSet<>(requestedProductIds);
        order.getItems().removeIf(item -> !requestedProductIdSet.contains(item.getProductId()));

        for (OrderItemRequest itemRequest : requestedItems) {
            Product product = productsById.get(itemRequest.productId());
            OrderItem item = existingItems.get(itemRequest.productId());
            if (item == null) {
                item = new OrderItem();
                item.setOrder(order);
                item.setProductId(product.getId());
                order.getItems().add(item);
            }
            applyItemValues(item, itemRequest, product, mappingsByProductId.get(product.getId()).getProductName());
        }
    }

    private void applyItemValues(OrderItem item, OrderItemRequest request, Product product, String productName) {
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Unit price must be greater than or equal to 0");
        }

        item.setProductCode(product.getNavItemCode());
        item.setProductDescription(productName);
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setLineRemark(cleanOptional(request.remark()));
        item.setLineTotal(request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP));
    }

    private List<Long> distinctProductIds(List<OrderItemRequest> requestedItems) {
        Set<Long> seen = new LinkedHashSet<>();
        for (OrderItemRequest item : requestedItems) {
            if (item.productId() == null) {
                throw new BadRequestException("Product id is required");
            }
            if (!seen.add(item.productId())) {
                throw new BadRequestException("Duplicate product id in order: " + item.productId());
            }
        }
        return new ArrayList<>(seen);
    }

    private void syncApprovers(Order order,
                               List<OrderApproverRequest> requestedApprovers,
                               UUID businessCustomerId,
                               UUID creatorId,
                               OrderApprovalPolicyConfig policy) {
        if (requestedApprovers == null) {
            return;
        }
        if (requestedApprovers.isEmpty()) {
            order.getApprovers().clear();
            return;
        }

        List<UUID> normalizedApproverIds = distinctApproverIds(
                requestedApprovers.stream().map(OrderApproverRequest::userId).toList());
        List<UserRepository.ApproverEligibilityView> eligibilityRows = userRepository.findApproverEligibility(
                normalizedApproverIds, businessCustomerId, UserStatus.ACTIVE, ORDER_APPROVE);
        Map<UUID, Set<String>> rolesByUser = eligibilityRows.stream().collect(Collectors.groupingBy(
                UserRepository.ApproverEligibilityView::getUserId,
                Collectors.mapping(UserRepository.ApproverEligibilityView::getRoleName, Collectors.toSet())
        ));
        Set<UUID> eligibleApproverIds = rolesByUser.keySet();
        List<UUID> invalidApproverIds = normalizedApproverIds.stream()
                .filter(approverId -> !eligibleApproverIds.contains(approverId))
                .toList();
        if (!invalidApproverIds.isEmpty()) {
            throw new ValidationException("Invalid approvers", Map.of("approverIds", invalidApproverIds.toString()));
        }
        if (!policy.selfApprovalAllowed() && normalizedApproverIds.contains(creatorId)) {
            throw new BadRequestException("Order creator cannot be assigned as an approver for this customer");
        }
        validateApproverLevels(requestedApprovers, policy);
        validateApproverRoles(requestedApprovers, policy, rolesByUser);

        Map<UUID, OrderApprover> existingApprovers = order.getApprovers().stream()
                .collect(Collectors.toMap(OrderApprover::getUserId, Function.identity()));
        Set<UUID> requestedApproverIdSet = new HashSet<>(normalizedApproverIds);
        order.getApprovers().removeIf(approver -> !requestedApproverIdSet.contains(approver.getUserId()));

        for (OrderApproverRequest assignment : requestedApprovers) {
            UUID approverId = assignment.userId();
            OrderApprover existing = existingApprovers.get(approverId);
            if (existing == null) {
                OrderApprover approver = new OrderApprover();
                approver.setOrder(order);
                approver.setUserId(approverId);
                approver.setApprovalLevel(assignment.approvalLevel());
                approver.setApprovalStatus(ApprovalStatus.PENDING);
                order.getApprovers().add(approver);
            } else if (existing.getApprovalLevel() != assignment.approvalLevel()) {
                existing.setApprovalLevel(assignment.approvalLevel());
                existing.setApprovalStatus(ApprovalStatus.PENDING);
                existing.setRemark(null);
                existing.setActedAt(null);
            }
        }
    }

    private List<OrderApproverRequest> requestedApprovers(List<OrderApproverRequest> assignments,
                                                           List<UUID> legacyApproverIds) {
        if (assignments != null) {
            return assignments;
        }
        if (legacyApproverIds == null) {
            return null;
        }
        return legacyApproverIds.stream().map(id -> new OrderApproverRequest(id, 1)).toList();
    }

    private void validateApproverLevels(List<OrderApproverRequest> assignments,
                                        OrderApprovalPolicyConfig policy) {
        Map<Integer, Long> assignedByLevel = assignments.stream().collect(Collectors.groupingBy(
                OrderApproverRequest::approvalLevel, Collectors.counting()));
        Set<Integer> configuredLevels = policy.levels().stream()
                .map(ApprovalLevelPolicy::levelNumber).collect(Collectors.toSet());
        List<Integer> invalidLevels = assignedByLevel.keySet().stream()
                .filter(level -> !configuredLevels.contains(level)).sorted().toList();
        if (!invalidLevels.isEmpty()) {
            throw new BadRequestException("Approvers contain unconfigured levels: " + invalidLevels);
        }
        for (ApprovalLevelPolicy level : policy.levels()) {
            long assigned = assignedByLevel.getOrDefault(level.levelNumber(), 0L);
            if (assigned < level.minimumApprovers()) {
                throw new BadRequestException("Approval level " + level.levelNumber()
                        + " requires at least " + level.minimumApprovers() + " approver(s)");
            }
        }
    }

    private void validateApproverRoles(List<OrderApproverRequest> assignments,
                                       OrderApprovalPolicyConfig policy,
                                       Map<UUID, Set<String>> rolesByUser) {
        Map<Integer, ApprovalLevelPolicy> policiesByLevel = policy.levels().stream()
                .collect(Collectors.toMap(ApprovalLevelPolicy::levelNumber, Function.identity()));
        for (OrderApproverRequest assignment : assignments) {
            Set<String> requiredRoles = policiesByLevel.get(assignment.approvalLevel()).eligibleRoles();
            if (requiredRoles == null || requiredRoles.isEmpty()) {
                continue;
            }
            Set<String> normalizedRequiredRoles = requiredRoles.stream()
                    .filter(Objects::nonNull)
                    .map(role -> role.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (rolesByUser.getOrDefault(assignment.userId(), Set.of()).stream()
                    .noneMatch(normalizedRequiredRoles::contains)) {
                throw new BadRequestException("Approver " + assignment.userId()
                        + " is not eligible for approval level " + assignment.approvalLevel());
            }
        }
    }

    private List<UUID> distinctApproverIds(List<UUID> approverIds) {
        Set<UUID> seen = new LinkedHashSet<>();
        for (UUID approverId : approverIds) {
            if (approverId == null) {
                throw new BadRequestException("Approver id cannot be null");
            }
            if (!seen.add(approverId)) {
                throw new BadRequestException("Duplicate approver id in order: " + approverId);
            }
        }
        return new ArrayList<>(seen);
    }

    private List<UUID> eligibleApproverIds(Collection<UUID> approverIds, UUID businessCustomerId, Collection<String> permissionCodes) {
        if (approverIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findEligibleApproverIds(
                approverIds,
                businessCustomerId,
                UserStatus.ACTIVE,
                permissionCodes
        );
    }

    private List<String> approvalPermission(ApprovalAction action) {
        if (action == ApprovalAction.REJECT) {
            return List.of(ORDER_REJECT);
        }
        return List.of(ORDER_APPROVE);
    }

    private void assertEligibleApprover(UUID userId, UUID businessCustomerId, Collection<String> permissionCodes) {
        if (eligibleApproverIds(List.of(userId), businessCustomerId, permissionCodes).isEmpty()) {
            throw new ForbiddenException("Approver is no longer eligible for this order");
        }
    }

    private boolean isAssignedEligibleApprover(Order order, UUID userId) {
        boolean assigned = order.getApprovers().stream().anyMatch(approver -> approver.getUserId().equals(userId));
        return assigned && !eligibleApproverIds(List.of(userId), order.getBusinessCustomerId(), List.of(ORDER_APPROVE)).isEmpty();
    }

    private void resetApprovalActivity(Order order) {
        boolean hasApprovalActivity = order.getApprovers().stream()
                .anyMatch(approver -> approver.getApprovalStatus() != ApprovalStatus.PENDING);
        if (!hasApprovalActivity) {
            return;
        }
        for (OrderApprover approver : order.getApprovers()) {
            approver.setApprovalStatus(ApprovalStatus.PENDING);
            approver.setRemark(null);
            approver.setActedAt(null);
        }
    }

    private void assertApproverCanAct(Order order, OrderApprover approver) {
        OrderApprovalPolicyConfig policy = policySnapshot(order);
        if (levelSatisfied(order, approver.getApprovalLevel(), policy)) {
            throw new BadRequestException("Approval level " + approver.getApprovalLevel() + " is already complete");
        }
        if (policy.approvalMode() == ApprovalMode.SEQUENTIAL) {
            boolean earlierIncomplete = policy.levels().stream()
                    .filter(level -> level.levelNumber() < approver.getApprovalLevel())
                    .anyMatch(level -> !levelSatisfied(order, level.levelNumber(), policy));
            if (earlierIncomplete) {
                throw new BadRequestException("Earlier approval levels must be completed first");
            }
        }
    }

    private boolean approvalRequirementsSatisfied(Order order) {
        OrderApprovalPolicyConfig policy = policySnapshot(order);
        return policy.levels().stream().allMatch(level -> levelSatisfied(order, level.levelNumber(), policy));
    }

    private boolean levelSatisfied(Order order, int levelNumber, OrderApprovalPolicyConfig policy) {
        ApprovalLevelPolicy rule = policy.levels().stream()
                .filter(level -> level.levelNumber() == levelNumber).findFirst()
                .orElseThrow(() -> new BadRequestException("Approval level is not configured"));
        List<OrderApprover> assigned = order.getApprovers().stream()
                .filter(approver -> approver.getApprovalLevel() == levelNumber).toList();
        if (assigned.size() < rule.minimumApprovers()) {
            return false;
        }
        long approved = assigned.stream()
                .filter(approver -> approver.getApprovalStatus() == ApprovalStatus.APPROVED).count();
        return rule.completionRule() == ApprovalCompletionRule.ALL
                ? approved == assigned.size()
                : approved >= 1;
    }

    private OrderApprovalPolicyConfig policySnapshot(Order order) {
        return order.getApprovalPolicySnapshot() == null
                ? OrderApprovalPolicyConfig.defaultPolicy()
                : order.getApprovalPolicySnapshot();
    }

    private boolean orderIsComplete(Order order) {
        return !order.getItems().isEmpty() && !order.getApprovers().isEmpty();
    }

    private void assertEditable(Order order) {
        if (!EDITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is no longer editable");
        }
    }

    private void requireStatus(Order order, OrderStatus status, String message) {
        if (order.getStatus() != status) {
            throw new BadRequestException(message);
        }
    }

    private LocalDate validExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        if (expectedDeliveryDate == null) {
            throw new BadRequestException("expectedDeliveryDate is required");
        }
        if (expectedDeliveryDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("expectedDeliveryDate cannot be in the past");
        }
        return expectedDeliveryDate;
    }

    private void applyFields(Order order, String notes, String remarks, String priority, String location, String referenceNumber) {
        order.setNotes(cleanOptional(notes));
        order.setRemarks(cleanOptional(remarks));
        order.setPriority(cleanOptional(priority));
        order.setLocation(cleanOptional(location));
        order.setReferenceNumber(cleanOptional(referenceNumber));
    }

    private void applyOrderLocation(Order order,
                                    User actor,
                                    BusinessCustomer customer,
                                    UUID requestedLocationId,
                                    String requestedLocationCode,
                                    String requestedLocationText,
                                    boolean useActorDefault) {
        BusinessCustomerLocation location = resolveOrderLocation(
                actor,
                customer,
                requestedLocationId,
                requestedLocationCode,
                useActorDefault
        );
        if (location != null) {
            order.setBusinessCustomerLocationId(location.getId());
            order.setLocation(locationSummary(location));
            return;
        }
        if (cleanOptional(requestedLocationText) != null) {
            order.setBusinessCustomerLocationId(null);
            return;
        }
        if (order.getBusinessCustomerLocationId() != null) {
            businessCustomerLocationRepository.findById(order.getBusinessCustomerLocationId())
                    .ifPresent(existingLocation -> order.setLocation(locationSummary(existingLocation)));
        }
    }

    private BusinessCustomerLocation resolveOrderLocation(User actor,
                                                         BusinessCustomer customer,
                                                         UUID requestedLocationId,
                                                         String requestedLocationCode,
                                                         boolean useActorDefault) {
        UUID effectiveLocationId = requestedLocationId;
        String effectiveLocationCode = cleanOptional(requestedLocationCode);
        if (effectiveLocationId == null && effectiveLocationCode == null && useActorDefault) {
            effectiveLocationId = actor.getBusinessCustomerLocationId();
        }

        BusinessCustomerLocation byId = null;
        if (effectiveLocationId != null) {
            byId = activeLocation(effectiveLocationId);
            assertLocationBelongsToCustomer(byId, customer.getId());
        }

        BusinessCustomerLocation byCode = null;
        if (effectiveLocationCode != null) {
            byCode = businessCustomerLocationRepository.findByBusinessCustomerIdAndLocationCode(
                            customer.getId(),
                            effectiveLocationCode.trim().toUpperCase(Locale.ROOT)
                    )
                    .filter(location -> location.getStatus() == BusinessCustomerLocationStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Business customer location not found"));
        }

        if (byId != null && byCode != null && !byId.getId().equals(byCode.getId())) {
            throw new BadRequestException("businessCustomerLocationId and locationCode refer to different locations");
        }
        return byId != null ? byId : byCode;
    }

    private BusinessCustomerLocation activeLocation(UUID locationId) {
        BusinessCustomerLocation location = businessCustomerLocationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer location not found"));
        if (location.getStatus() != BusinessCustomerLocationStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer location not found");
        }
        return location;
    }

    private void assertLocationBelongsToCustomer(BusinessCustomerLocation location, UUID businessCustomerId) {
        if (!businessCustomerId.equals(location.getBusinessCustomerId())) {
            throw new ForbiddenException("Location belongs to another business customer");
        }
    }

    private String locationSummary(BusinessCustomerLocation location) {
        List<String> parts = new ArrayList<>();
        parts.add(location.getLocationCode());
        parts.add(location.getLocationName());
        parts.add(location.getCity());
        return parts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" - "));
    }

    private Order findDetailedForUpdate(UUID orderId) {
        if (orderId == null) {
            throw new BadRequestException("Order id is required");
        }
        return orderRepository.findDetailedByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private User currentUser() {
        UUID currentUserId = SecurityContextHelper.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BusinessCustomer creatorBusinessCustomer(User creator) {
        if (creator.getBusinessCustomerId() == null) {
            throw new ForbiddenException("Only users assigned to a business customer can create orders");
        }
        BusinessCustomer customer = businessCustomer(creator.getBusinessCustomerId());
        if (!customer.getOrganizationId().equals(creator.getOrganizationId())) {
            throw new ForbiddenException("User and business customer belong to different organizations");
        }
        if (creator.getBranchId() != null && !customer.getBranchId().equals(creator.getBranchId())) {
            throw new ForbiddenException("User and business customer belong to different branches");
        }
        return customer;
    }

    private BusinessCustomer businessCustomer(UUID businessCustomerId) {
        BusinessCustomer customer = businessCustomerRepository.findById(businessCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
        if (customer.getStatus() != BusinessCustomerStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer not found");
        }
        return customer;
    }

    private String defaultLocationName(UUID businessCustomerLocationId) {
        if (businessCustomerLocationId == null) {
            return null;
        }
        return businessCustomerLocationRepository.findById(businessCustomerLocationId)
                .map(location -> location.getLocationName())
                .orElse(null);
    }

    private void assertCustomerSideMutationAllowed(Order order, User actor) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return;
        }
        if (actor.getBusinessCustomerId() != null && actor.getBusinessCustomerId().equals(order.getBusinessCustomerId())) {
            return;
        }
        if (actor.getId().equals(order.getCreatedBy())) {
            return;
        }
        throw new ForbiddenException("Cannot modify order from another business customer");
    }

    private void assertSupplierActionAllowed(User actor, SupplierAction action) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return;
        }
        Organization organization = organization(actor.getOrganizationId());
        if (!isSupplierSideOrganization(organization.getOrganizationType())) {
            throw new ForbiddenException("Supplier actions are not allowed for customer users");
        }
        if (!SecurityContextHelper.hasRole(ORGANIZATION_ADMIN) && !SecurityContextHelper.hasRole(BRANCH_ADMIN)) {
            throw new ForbiddenException("Only organization admin or branch admin can perform supplier actions");
        }
        Set<String> permissions = new HashSet<>(userRoleRepository.findPermissionCodesByUserId(actor.getId()));
        String requiredPermission = action == SupplierAction.REJECT ? ORDER_REJECT : ORDER_UPDATE;
        if (!permissions.contains(requiredPermission)) {
            throw new ForbiddenException("Supplier order permission is required");
        }
    }

    private void assertVisible(Order order, User user) {
        if (!canView(order, user)) {
            throw new ForbiddenException("Cannot access this order");
        }
    }

    private boolean canView(Order order, User user) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return true;
        }
        if (user.getBusinessCustomerId() != null && user.getBusinessCustomerId().equals(order.getBusinessCustomerId())) {
            return true;
        }

        Organization organization = organization(user.getOrganizationId());
        if (isSupplierSideOrganization(organization.getOrganizationType())) {
            return SUPPLIER_VISIBLE_STATUSES.contains(order.getStatus());
        }
        if (organization.getOrganizationType() == OrganizationType.CUSTOMER
                && Objects.equals(user.getOrganizationId(), order.getOrganizationId())) {
            return user.getBranchId() == null || Objects.equals(user.getBranchId(), order.getBranchId());
        }
        return false;
    }

    private Specification<Order> visibleTo(User user) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return alwaysTrue();
        }

        Organization organization = organization(user.getOrganizationId());
        if (user.getBusinessCustomerId() != null) {
            UUID businessCustomerId = user.getBusinessCustomerId();
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("businessCustomerId"), businessCustomerId);
        }
        if (isSupplierSideOrganization(organization.getOrganizationType())) {
            return (root, query, criteriaBuilder) -> root.get("status").in(SUPPLIER_VISIBLE_STATUSES);
        }
        UUID organizationId = user.getOrganizationId();
        UUID branchId = user.getBranchId();
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organizationId"), organizationId));
            if (branchId != null) {
                predicates.add(criteriaBuilder.equal(root.get("branchId"), branchId));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Order> matchesStatus(OrderStatus status) {
        if (status == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<Order> matchesCreatedBy(UUID createdBy) {
        if (createdBy == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createdBy"), createdBy);
    }

    private Specification<Order> matchesBusinessCustomer(UUID businessCustomerId) {
        if (businessCustomerId == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("businessCustomerId"), businessCustomerId);
    }

    private Specification<Order> matchesOrderNumber(String orderNumber) {
        String value = cleanOptional(orderNumber);
        if (value == null) {
            return alwaysTrue();
        }
        String pattern = "%" + value.toLowerCase(Locale.ROOT) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("orderNumber")),
                pattern
        );
    }

    private Specification<Order> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private Organization organization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private boolean isSupplierSideOrganization(OrganizationType organizationType) {
        return organizationType == OrganizationType.SUPPLIER
                || organizationType == OrganizationType.SYSTEM
                || organizationType == OrganizationType.PARENT;
    }

    private String companyCode(BusinessCustomer customer) {
        String source = customer.getCustomerCode();
        if (source == null || source.isBlank()) {
            source = customer.getId().toString();
        }
        String normalized = source.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            normalized = customer.getId().toString().replace("-", "").toUpperCase(Locale.ROOT);
        }
        return (normalized + "XXX").substring(0, 3);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductCode(),
                        item.getProductDescription(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineRemark(),
                        item.getLineTotal()
                ))
                .toList();
        List<OrderApproverResponse> approverResponses = order.getApprovers().stream()
                .map(approver -> new OrderApproverResponse(
                        approver.getId(),
                        approver.getUserId(),
                        approver.getApprovalLevel(),
                        approver.getApprovalStatus(),
                        approver.getRemark(),
                        approver.getActedAt()
                ))
                .toList();
        BigDecimal totalAmount = itemResponses.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrganizationId(),
                order.getBranchId(),
                order.getBusinessCustomerId(),
                order.getBusinessCustomerLocationId(),
                order.getBusinessCustomerCode(),
                order.getBusinessCustomerName(),
                order.getCreatedBy(),
                order.getNotes(),
                order.getRemarks(),
                order.getPriority(),
                order.getLocation(),
                locationResponse(order.getBusinessCustomerLocationId()),
                order.getReferenceNumber(),
                order.getStatus(),
                order.getExpectedDeliveryDate(),
                totalAmount,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                approverResponses
        );
    }

    private OrderLocationResponse locationResponse(UUID locationId) {
        if (locationId == null) {
            return null;
        }
        return businessCustomerLocationRepository.findById(locationId)
                .map(location -> new OrderLocationResponse(
                        location.getId(),
                        location.getBusinessCustomerId(),
                        location.getOrganizationId(),
                        location.getBranchId(),
                        location.getLocationCode(),
                        location.getLocationName(),
                        location.getCity(),
                        location.getState(),
                        location.getAddress(),
                        location.getPermanentAddress(),
                        location.getCorrespondingAddress(),
                        location.isSameAsPermanentAddress(),
                        location.getPincode()
                ))
                .orElse(null);
    }

    private void audit(Order order, String action, String description, HttpServletRequest servletRequest) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("orderNumber", order.getOrderNumber());
        values.put("status", order.getStatus().name());
        auditService.record(
                action,
                order.getOrganizationId(),
                SecurityContextHelper.getCurrentUserId(),
                "Order",
                order.getId(),
                description,
                null,
                values,
                servletRequest
        );
    }

    private void publish(String action, Order order, UUID actorUserId) {
        eventPublisher.publishEvent(new OrderEvent(
                action,
                order.getId(),
                order.getOrderNumber(),
                order.getBusinessCustomerId(),
                actorUserId
        ));
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
