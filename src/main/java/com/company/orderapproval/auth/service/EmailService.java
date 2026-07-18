package com.company.orderapproval.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String email, String resetLink);
}
