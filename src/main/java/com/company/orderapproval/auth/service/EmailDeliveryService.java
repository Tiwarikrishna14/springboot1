package com.company.orderapproval.auth.service;

public interface EmailDeliveryService {
    void sendPasswordResetEmail(String email, String resetLink);
}
