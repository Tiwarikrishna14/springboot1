package com.company.orderapproval.auth.controller;

import com.company.orderapproval.auth.dto.AuthResponse;
import com.company.orderapproval.auth.dto.AuthUserResponse;
import com.company.orderapproval.auth.dto.ForgotPasswordRequest;
import com.company.orderapproval.auth.dto.LoginRequest;
import com.company.orderapproval.auth.dto.LogoutRequest;
import com.company.orderapproval.auth.dto.RefreshTokenRequest;
import com.company.orderapproval.auth.dto.RegisterRequest;
import com.company.orderapproval.auth.dto.ResetPasswordRequest;
import com.company.orderapproval.auth.service.AuthService;
import com.company.orderapproval.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register an organization admin")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authService.register(request, servletRequest)));
    }

    @Operation(summary = "Login and receive access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request, servletRequest)));
    }

    @Operation(summary = "Rotate a refresh token and receive new tokens")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                             HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authService.refresh(request, servletRequest)));
    }

    @Operation(summary = "Revoke a refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request,
                                                    HttpServletRequest servletRequest) {
        authService.logout(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                            HttpServletRequest servletRequest) {
        authService.forgotPassword(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a reset link will be sent"));
    }

    @Operation(summary = "Reset password using a reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                           HttpServletRequest servletRequest) {
        authService.resetPassword(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
    }

    @Operation(summary = "Get current authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success("Current user fetched successfully", authService.currentUser().user()));
    }
}
