package com.company.orderapproval.auth.security;

import com.company.orderapproval.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessExpirationSeconds());
        return Jwts.builder()
                .subject(user.email())
                .claim("userId", user.userId().toString())
                .claim("organizationId", user.organizationId().toString())
                .claim("email", user.email())
                .claim("roles", user.roles())
                .claim("permissions", user.permissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseAuthenticatedUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(
                    UUID.fromString(claims.get("userId", String.class)),
                    UUID.fromString(claims.get("organizationId", String.class)),
                    claims.get("email", String.class),
                    claimList(claims, "roles"),
                    claimList(claims, "permissions")
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired JWT token");
        }
    }

    public long accessTokenExpiresInSeconds() {
        return jwtProperties.accessExpirationSeconds();
    }

    public long refreshTokenExpiresInSeconds() {
        return jwtProperties.refreshExpirationSeconds();
    }

    @SuppressWarnings("unchecked")
    private List<String> claimList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }
}
