package com.company.orderapproval.auth.service;

import com.company.orderapproval.auth.dto.AuthResponse;
import com.company.orderapproval.auth.dto.ForgotPasswordRequest;
import com.company.orderapproval.auth.dto.LoginRequest;
import com.company.orderapproval.auth.dto.LogoutRequest;
import com.company.orderapproval.auth.dto.RefreshTokenRequest;
import com.company.orderapproval.auth.dto.RegisterRequest;
import com.company.orderapproval.auth.dto.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest);

    AuthResponse login(LoginRequest request, HttpServletRequest servletRequest);

    AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest);

    void logout(LogoutRequest request, HttpServletRequest servletRequest);

    void forgotPassword(ForgotPasswordRequest request, HttpServletRequest servletRequest);

    void resetPassword(ResetPasswordRequest request, HttpServletRequest servletRequest);

    AuthResponse currentUser();
}
