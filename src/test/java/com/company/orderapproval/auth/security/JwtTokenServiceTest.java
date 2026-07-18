package com.company.orderapproval.auth.security;

import com.company.orderapproval.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void generatesAndValidatesJwt() {
        JwtTokenService jwtTokenService = new JwtTokenService(new JwtProperties(
                "unit-test-secret-key-with-at-least-32-bytes",
                900,
                604800
        ));
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                organizationId,
                "admin@example.com",
                List.of("ORGANIZATION_ADMIN"),
                List.of("USER_VIEW", "USER_CREATE")
        );

        String token = jwtTokenService.generateAccessToken(user);
        AuthenticatedUser parsed = jwtTokenService.parseAuthenticatedUser(token);

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.organizationId()).isEqualTo(organizationId);
        assertThat(parsed.email()).isEqualTo("admin@example.com");
        assertThat(parsed.roles()).containsExactly("ORGANIZATION_ADMIN");
        assertThat(parsed.permissions()).containsExactly("USER_VIEW", "USER_CREATE");
    }

    @Test
    void rejectsInvalidJwt() {
        JwtTokenService jwtTokenService = new JwtTokenService(new JwtProperties(
                "unit-test-secret-key-with-at-least-32-bytes",
                900,
                604800
        ));

        assertThatThrownBy(() -> jwtTokenService.parseAuthenticatedUser("not-a-jwt"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
