package com.company.orderapproval.customer.location.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ForbiddenException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.customer.entity.BusinessCustomer;
import com.company.orderapproval.customer.entity.BusinessCustomerStatus;
import com.company.orderapproval.customer.location.dto.BusinessCustomerLocationResponse;
import com.company.orderapproval.customer.location.dto.CreateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.dto.UpdateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocation;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus;
import com.company.orderapproval.customer.location.repository.BusinessCustomerLocationRepository;
import com.company.orderapproval.customer.repository.BusinessCustomerRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class BusinessCustomerLocationServiceImpl implements BusinessCustomerLocationService {

    private final BusinessCustomerLocationRepository repo;
    private final BusinessCustomerRepository customers;
    private final UserRepository users;

    public BusinessCustomerLocationServiceImpl(BusinessCustomerLocationRepository repo,
                                               BusinessCustomerRepository customers,
                                               UserRepository users) {
        this.repo = repo;
        this.customers = customers;
        this.users = users;
    }

    @Override
    public Page<BusinessCustomerLocationResponse> list(UUID customerId,
                                                       String search,
                                                       BusinessCustomerLocationStatus status,
                                                       Pageable pageable) {
        if (status != null && status != BusinessCustomerLocationStatus.ACTIVE) {
            return Page.empty(pageable);
        }
        BusinessCustomer customer = customer(customerId);
        scope(customer);

        UUID currentLocation = currentLocationScope();
        if (currentLocation != null) {
            return repo.findById(currentLocation)
                    .filter(location -> location.getStatus() == BusinessCustomerLocationStatus.ACTIVE)
                    .filter(location -> location.getBusinessCustomerId().equals(customerId))
                    .map(location -> new PageImpl<>(List.of(location), pageable, 1))
                    .orElse(new PageImpl<>(List.of(), pageable, 0))
                    .map(this::response);
        }
        return repo.search(customerId, search, BusinessCustomerLocationStatus.ACTIVE, pageable).map(this::response);
    }

    @Override
    public Page<BusinessCustomerLocationResponse> listByCustomerCode(UUID organizationId,
                                                                     String customerCode,
                                                                     String search,
                                                                     BusinessCustomerLocationStatus status,
                                                                     Pageable pageable) {
        if (customerCode == null || customerCode.isBlank()) {
            throw new BadRequestException("customerCode is required");
        }
        if (status != null && status != BusinessCustomerLocationStatus.ACTIVE) {
            return Page.empty(pageable);
        }
        BusinessCustomer customer = customerByCode(organizationId, customerCode);
        scope(customer);
        return list(customer.getId(), search, BusinessCustomerLocationStatus.ACTIVE, pageable);
    }

    @Override
    public BusinessCustomerLocationResponse get(UUID id) {
        BusinessCustomerLocation location = access(id);
        if (location.getStatus() != BusinessCustomerLocationStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer location not found");
        }
        return response(location);
    }

    @Override
    @Transactional
    public BusinessCustomerLocationResponse create(UUID customerId,
                                                   CreateBusinessCustomerLocationRequest request,
                                                   HttpServletRequest httpServletRequest) {
        BusinessCustomer customer = customer(customerId);
        scope(customer);
        String code = request.locationCode().trim().toUpperCase(Locale.ROOT);
        if (repo.findByBusinessCustomerIdAndLocationCode(customerId, code).isPresent()) {
            throw new ConflictException("Location code already exists for this customer");
        }

        BusinessCustomerLocation location = new BusinessCustomerLocation();
        location.setBusinessCustomerId(customer.getId());
        location.setOrganizationId(customer.getOrganizationId());
        location.setBranchId(customer.getBranchId());
        location.setLocationCode(code);
        applyRequest(location, request.locationName(), request.city(), request.state(), request.address(),
                request.permanentAddress(), request.correspondingAddress(), request.sameAsPermanentAddress(),
                request.pincode());
        repo.save(location);
        return response(location);
    }

    @Override
    @Transactional
    public BusinessCustomerLocationResponse update(UUID id,
                                                   UpdateBusinessCustomerLocationRequest request,
                                                   HttpServletRequest httpServletRequest) {
        BusinessCustomerLocation location = access(id);
        applyRequest(location, request.locationName(), request.city(), request.state(), request.address(),
                request.permanentAddress(), request.correspondingAddress(), request.sameAsPermanentAddress(),
                request.pincode());
        location.setStatus(request.status());
        repo.save(location);
        return response(location);
    }

    private void applyRequest(BusinessCustomerLocation location,
                              String locationName,
                              String city,
                              String state,
                              String address,
                              String permanentAddress,
                              String correspondingAddress,
                              Boolean sameAsPermanentAddress,
                              String pincode) {
        boolean sameAsPermanent = Boolean.TRUE.equals(sameAsPermanentAddress);
        String cleanPermanentAddress = firstNonBlank(permanentAddress, address);
        String cleanCorrespondingAddress = sameAsPermanent
                ? cleanPermanentAddress
                : cleanOptional(correspondingAddress);

        location.setLocationName(locationName.trim());
        location.setCity(city.trim());
        location.setState(cleanOptional(state));
        location.setAddress(firstNonBlank(address, cleanPermanentAddress));
        location.setPermanentAddress(cleanPermanentAddress);
        location.setCorrespondingAddress(cleanCorrespondingAddress);
        location.setSameAsPermanentAddress(sameAsPermanent);
        location.setPincode(cleanOptional(pincode));
    }

    private BusinessCustomer customer(UUID id) {
        BusinessCustomer customer = customers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer not found"));
        if (customer.getStatus() != BusinessCustomerStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business customer not found");
        }
        return customer;
    }

    private BusinessCustomer customerByCode(UUID requestedOrganizationId, String customerCode) {
        UUID organizationId = SecurityContextHelper.isSuperAdmin()
                ? requestedOrganizationId
                : SecurityContextHelper.getCurrentOrganizationId();
        List<BusinessCustomer> matches = customers.findByCustomerCodeInScope(
                organizationId,
                customerCode.trim().toUpperCase(Locale.ROOT),
                BusinessCustomerStatus.ACTIVE
        );
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Business customer not found");
        }
        if (matches.size() > 1) {
            throw new BadRequestException("Multiple business customers found for this customerCode. Pass organizationId to identify one customer.");
        }
        return matches.get(0);
    }

    private void scope(BusinessCustomer customer) {
        if (SecurityContextHelper.isSuperAdmin()) {
            return;
        }
        if (!customer.getOrganizationId().equals(SecurityContextHelper.getCurrentOrganizationId())) {
            throw new ForbiddenException("Cannot access another organization");
        }
        User user = users.findById(SecurityContextHelper.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (SecurityContextHelper.hasRole("ORGANIZATION_ADMIN")) {
            return;
        }
        if (user.getBranchId() != null && !Objects.equals(user.getBranchId(), customer.getBranchId())) {
            throw new ForbiddenException("Cannot access another branch");
        }
        if (user.getBusinessCustomerId() != null && !user.getBusinessCustomerId().equals(customer.getId())) {
            throw new ForbiddenException("Cannot access another business customer");
        }
    }

    private BusinessCustomerLocation access(UUID id) {
        BusinessCustomerLocation location = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business customer location not found"));
        BusinessCustomer customer = customer(location.getBusinessCustomerId());
        scope(customer);
        UUID currentLocation = currentLocationScope();
        if (currentLocation != null && !currentLocation.equals(id)) {
            throw new ForbiddenException("Cannot access another business customer location");
        }
        return location;
    }

    private UUID currentLocationScope() {
        if (SecurityContextHelper.isSuperAdmin() || SecurityContextHelper.hasRole("ORGANIZATION_ADMIN")) {
            return null;
        }
        return users.findById(SecurityContextHelper.getCurrentUserId())
                .map(User::getBusinessCustomerLocationId)
                .orElse(null);
    }

    private BusinessCustomerLocationResponse response(BusinessCustomerLocation location) {
        return new BusinessCustomerLocationResponse(
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
                location.getPincode(),
                location.getStatus(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }

    private String firstNonBlank(String first, String fallback) {
        String cleanedFirst = cleanOptional(first);
        return cleanedFirst == null ? cleanOptional(fallback) : cleanedFirst;
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
