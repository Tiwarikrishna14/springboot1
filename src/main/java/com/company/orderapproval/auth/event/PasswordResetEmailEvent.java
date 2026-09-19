package com.company.orderapproval.auth.event;

public record PasswordResetEmailEvent(String email, String resetLink) {
}
