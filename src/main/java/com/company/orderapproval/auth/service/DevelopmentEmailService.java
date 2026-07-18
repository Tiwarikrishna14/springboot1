package com.company.orderapproval.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class DevelopmentEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentEmailService.class);

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        log.info("Development password reset URL for {}: {}", email, resetLink);
    }
}
