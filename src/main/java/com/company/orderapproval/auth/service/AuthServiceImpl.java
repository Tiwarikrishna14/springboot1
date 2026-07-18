package com.company.orderapproval.auth.service;

import com.company.orderapproval.audit.service.AuditService;
import com.company.orderapproval.auth.dto.AuthResponse;
import com.company.orderapproval.auth.dto.AuthUserResponse;
import com.company.orderapproval.auth.dto.ForgotPasswordRequest;
import com.company.orderapproval.auth.dto.LoginRequest;
import com.company.orderapproval.auth.dto.LogoutRequest;
import com.company.orderapproval.auth.dto.RefreshTokenRequest;
import com.company.orderapproval.auth.dto.RegisterRequest;
import com.company.orderapproval.auth.dto.ResetPasswordRequest;
import com.company.orderapproval.auth.repository.PasswordResetTokenRepository;
import com.company.orderapproval.auth.repository.RefreshTokenRepository;
import com.company.orderapproval.auth.security.AuthenticatedUser;
import com.company.orderapproval.auth.security.CustomUserDetailsService;
import com.company.orderapproval.auth.security.JwtTokenService;
import com.company.orderapproval.auth.security.PasswordResetToken;
import com.company.orderapproval.auth.security.RefreshToken;
import com.company.orderapproval.common.constant.AuditActions;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.common.exception.ConflictException;
import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.exception.UnauthorizedException;
import com.company.orderapproval.common.util.HashUtil;
import com.company.orderapproval.common.util.IpAddressUtil;
import com.company.orderapproval.common.util.SecurityContextHelper;
import com.company.orderapproval.organization.entity.Organization;
import com.company.orderapproval.organization.entity.OrganizationStatus;
import com.company.orderapproval.organization.entity.OrganizationType;
import com.company.orderapproval.organization.repository.OrganizationRepository;
import com.company.orderapproval.role.entity.Role;
import com.company.orderapproval.role.repository.RoleRepository;
import com.company.orderapproval.user.entity.User;
import com.company.orderapproval.user.entity.UserRole;
import com.company.orderapproval.user.entity.UserStatus;
import com.company.orderapproval.user.repository.UserRepository;
import com.company.orderapproval.user.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_MINUTES = 15;
    private static final int PASSWORD_RESET_MINUTES = 30;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String passwordResetBaseUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RoleRepository roleRepository,
                           OrganizationRepository organizationRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService,
                           CustomUserDetailsService customUserDetailsService,
                           AuditService auditService,
                           EmailService emailService,
                           @Value("${app.password-reset.base-url}") String passwordResetBaseUrl) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.customUserDetailsService = customUserDetailsService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }

        Organization organization = new Organization();
        organization.setOrganizationCode(generateOrganizationCode(request.organizationName()));
        organization.setName(request.organizationName().trim());
        organization.setOrganizationType(OrganizationType.CUSTOMER);
        organization.setEmail(email);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organizationRepository.save(organization);

        User user = new User();
        user.setOrganizationId(organization.getId());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        userRepository.save(user);

        Role organizationAdminRole = roleRepository.findByNameAndOrganizationIdIsNull("ORGANIZATION_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Default organization admin role not found"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(organizationAdminRole);
        userRoleRepository.save(userRole);

        auditService.record(
                AuditActions.ORGANIZATION_CREATED,
                organization.getId(),
                user.getId(),
                "Organization",
                organization.getId(),
                "Organization registered",
                null,
                Map.of("organizationCode", organization.getOrganizationCode(), "name", organization.getName()),
                servletRequest
        );
        auditService.record(
                AuditActions.USER_CREATED,
                organization.getId(),
                user.getId(),
                "User",
                user.getId(),
                "Registration user created",
                null,
                Map.of("email", user.getEmail()),
                servletRequest
        );

        return issueTokens(user, servletRequest);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    auditService.record(AuditActions.LOGIN_FAILED, null, null, "User", null,
                            "Login failed for unknown email", null, Map.of("email", email), servletRequest);
                    return new UnauthorizedException("Invalid email or password");
                });

        unlockIfExpired(user);
        if (user.isLockedNow()) {
            auditService.record(AuditActions.LOGIN_FAILED, user.getOrganizationId(), user.getId(), "User", user.getId(),
                    "Login failed because account is locked", null, null, servletRequest);
            throw new UnauthorizedException("Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            auditService.record(AuditActions.LOGIN_FAILED, user.getOrganizationId(), user.getId(), "User", user.getId(),
                    "Login failed because account is not active", null, null, servletRequest);
            throw new UnauthorizedException("Account is not active");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user, servletRequest);
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.record(AuditActions.LOGIN_SUCCESS, user.getOrganizationId(), user.getId(), "User", user.getId(),
                "Login successful", null, null, servletRequest);
        return issueTokens(user, servletRequest);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(HashUtil.sha256(request.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!existingToken.isActive()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        existingToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existingToken);
        return issueTokens(existingToken.getUser(), servletRequest);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request, HttpServletRequest servletRequest) {
        refreshTokenRepository.findByTokenHash(HashUtil.sha256(request.refreshToken()))
                .ifPresent(refreshToken -> {
                    if (refreshToken.getRevokedAt() == null) {
                        refreshToken.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(refreshToken);
                    }
                    User user = refreshToken.getUser();
                    auditService.record(AuditActions.LOGOUT, user.getOrganizationId(), user.getId(), "User", user.getId(),
                            "Logout successful", null, null, servletRequest);
                });
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateOpaqueToken();
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setUser(user);
            passwordResetToken.setTokenHash(HashUtil.sha256(rawToken));
            passwordResetToken.setExpiresAt(Instant.now().plus(PASSWORD_RESET_MINUTES, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(passwordResetToken);

            String resetLink = passwordResetBaseUrl + "?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            auditService.record(AuditActions.PASSWORD_RESET_REQUEST, user.getOrganizationId(), user.getId(),
                    "User", user.getId(), "Password reset requested", null, null, servletRequest);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest servletRequest) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(HashUtil.sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));
        if (!token.isUsable()) {
            throw new BadRequestException("Reset token is expired or already used");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        token.setUsedAt(Instant.now());
        userRepository.save(user);
        passwordResetTokenRepository.save(token);
        auditService.record(AuditActions.PASSWORD_RESET_SUCCESS, user.getOrganizationId(), user.getId(), "User",
                user.getId(), "Password reset completed", null, null, servletRequest);
    }

    @Override
    public AuthResponse currentUser() {
        AuthenticatedUser principal = SecurityContextHelper.currentUser();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AuthResponse(null, null, "Bearer", jwtTokenService.accessTokenExpiresInSeconds(), toAuthUser(user, principal));
    }

    private AuthResponse issueTokens(User user, HttpServletRequest servletRequest) {
        AuthenticatedUser principal = customUserDetailsService.loadPrincipal(user);
        String accessToken = jwtTokenService.generateAccessToken(principal);
        String refreshTokenValue = generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(HashUtil.sha256(refreshTokenValue));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtTokenService.refreshTokenExpiresInSeconds()));
        refreshToken.setCreatedByIp(IpAddressUtil.extractClientIp(servletRequest));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtTokenService.accessTokenExpiresInSeconds(),
                toAuthUser(user, principal)
        );
    }

    private AuthUserResponse toAuthUser(User user, AuthenticatedUser principal) {
        return new AuthUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getOrganizationId(),
                principal.roles(),
                principal.permissions()
        );
    }

    private void registerFailedLogin(User user, HttpServletRequest servletRequest) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setStatus(UserStatus.LOCKED);
            user.setAccountLockedUntil(Instant.now().plus(ACCOUNT_LOCK_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
        auditService.record(AuditActions.LOGIN_FAILED, user.getOrganizationId(), user.getId(), "User", user.getId(),
                "Login failed", null, Map.of("failedLoginAttempts", attempts), servletRequest);
    }

    private void unlockIfExpired(User user) {
        if (user.getStatus() == UserStatus.LOCKED
                && user.getAccountLockedUntil() != null
                && user.getAccountLockedUntil().isBefore(Instant.now())) {
            user.setStatus(UserStatus.ACTIVE);
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private String generateOrganizationCode(String organizationName) {
        String base = organizationName.replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "ORG";
        }
        String suffix = String.format("%06X", secureRandom.nextInt(0x1000000));
        return (base.length() > 20 ? base.substring(0, 20) : base) + suffix;
    }

    private String generateOpaqueToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
