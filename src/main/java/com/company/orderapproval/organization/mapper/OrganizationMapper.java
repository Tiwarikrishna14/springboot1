package com.company.orderapproval.organization.mapper;

import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.dto.OrganizationResponse;
import com.company.orderapproval.organization.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Organization toEntity(CreateOrganizationRequest request);
}
