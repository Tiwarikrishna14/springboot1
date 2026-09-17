package com.company.orderapproval.customer.location.service;

import com.company.orderapproval.customer.location.dto.BusinessCustomerLocationResponse;
import com.company.orderapproval.customer.location.dto.CreateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.dto.UpdateBusinessCustomerLocationRequest;
import com.company.orderapproval.customer.location.entity.BusinessCustomerLocationStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BusinessCustomerLocationService {
    Page<BusinessCustomerLocationResponse> list(UUID customerId,
                                                String search,
                                                BusinessCustomerLocationStatus status,
                                                Pageable pageable);

    Page<BusinessCustomerLocationResponse> listByCustomerCode(UUID organizationId,
                                                              String customerCode,
                                                              String search,
                                                              BusinessCustomerLocationStatus status,
                                                              Pageable pageable);

    BusinessCustomerLocationResponse get(UUID id);

    BusinessCustomerLocationResponse create(UUID customerId,
                                            CreateBusinessCustomerLocationRequest request,
                                            HttpServletRequest httpServletRequest);

    BusinessCustomerLocationResponse update(UUID id,
                                            UpdateBusinessCustomerLocationRequest request,
                                            HttpServletRequest httpServletRequest);
}
