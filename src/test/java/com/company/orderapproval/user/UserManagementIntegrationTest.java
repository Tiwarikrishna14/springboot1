package com.company.orderapproval.user;

import com.company.orderapproval.AbstractIntegrationTest;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RoleRepository roleRepository;

    @Test
    void superAdminCanCreateUserAndAssignRole() throws Exception {
        UUID organizationId = createOrganization("USERCREATE");
        Role organizationAdminRole = roleRepository.findByNameAndOrganizationIdIsNull("ORGANIZATION_ADMIN")
                .orElseThrow();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "organizationId", organizationId.toString(),
                                "firstName", "Aman",
                                "lastName", "Manager",
                                "email", "aman.manager@example.com",
                                "password", "Password@123",
                                "roleIds", List.of(organizationAdminRole.getId().toString())
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("aman.manager@example.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ORGANIZATION_ADMIN"));
    }

    @Test
    void userWithoutPermissionCannotCreateUser() throws Exception {
        UUID organizationId = createOrganization("NOPERM");
        Role customerRole = roleRepository.findByNameAndOrganizationIdIsNull("CUSTOMER_USER")
                .orElseThrow();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "organizationId", organizationId.toString(),
                                "firstName", "Customer",
                                "lastName", "User",
                                "email", "customer.user@example.com",
                                "password", "Password@123",
                                "roleIds", List.of(customerRole.getId().toString())
                        ))))
                .andExpect(status().isCreated());

        String customerToken = loginAndGetAccessToken("customer.user@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", "Blocked",
                                "lastName", "User",
                                "email", "blocked.user@example.com",
                                "password", "Password@123"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationAdminCannotListUsersFromAnotherOrganization() throws Exception {
        UUID organizationOneId = createOrganization("TENANTA");
        UUID organizationTwoId = createOrganization("TENANTB");
        Role organizationAdminRole = roleRepository.findByNameAndOrganizationIdIsNull("ORGANIZATION_ADMIN")
                .orElseThrow();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "organizationId", organizationOneId.toString(),
                                "firstName", "Tenant",
                                "lastName", "Admin",
                                "email", "tenant.admin@example.com",
                                "password", "Password@123",
                                "roleIds", List.of(organizationAdminRole.getId().toString())
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "organizationId", organizationTwoId.toString(),
                                "firstName", "Other",
                                "lastName", "Tenant",
                                "email", "other.tenant@example.com",
                                "password", "Password@123"
                        ))))
                .andExpect(status().isCreated());

        String organizationAdminToken = loginAndGetAccessToken("tenant.admin@example.com", "Password@123");
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + organizationAdminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> emails = JsonPath.read(response, "$.data.content[*].email");
        assertThat(emails).contains("tenant.admin@example.com");
        assertThat(emails).doesNotContain("other.tenant@example.com");
    }
}
