package com.company.orderapproval;

import com.company.orderapproval.auth.dto.LoginRequest;
import com.company.orderapproval.common.response.ApiResponse;
import com.company.orderapproval.organization.dto.CreateOrganizationRequest;
import com.company.orderapproval.organization.entity.OrganizationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String SUPER_ADMIN_EMAIL = "superadmin@test.com";
    protected static final String SUPER_ADMIN_PASSWORD = "Password@123";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_approval_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String superAdminAccessToken;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.placeholders.initialAdminEmail", () -> SUPER_ADMIN_EMAIL);
        registry.add("spring.flyway.placeholders.initialAdminPassword", () -> SUPER_ADMIN_PASSWORD);
        registry.add("app.jwt.secret", () -> "integration-test-secret-key-with-at-least-32-bytes");
    }

    @BeforeEach
    void loginSuperAdmin() throws Exception {
        superAdminAccessToken = loginAndGetAccessToken(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);
    }

    protected String loginAndGetAccessToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }

    protected String loginAndGetRefreshToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.refreshToken");
    }

    protected UUID createOrganization(String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + superAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest(
                                code,
                                code + " Organization",
                                OrganizationType.CUSTOMER,
                                code.toLowerCase() + "@example.com",
                                null
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.data.id"));
    }

    protected String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
