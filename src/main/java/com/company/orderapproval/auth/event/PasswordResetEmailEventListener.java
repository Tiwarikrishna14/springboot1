package com.company.orderapproval.auth.event;

import com.company.orderapproval.auth.service.EmailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetEmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailEventListener.class);

    private final EmailDeliveryService emailDeliveryService;

    public PasswordResetEmailEventListener(EmailDeliveryService emailDeliveryService) {
        this.emailDeliveryService = emailDeliveryService;
    }

    @Async("eventExecutor")
    @EventListener
    public void handle(PasswordResetEmailEvent event) {
        try {
            emailDeliveryService.sendPasswordResetEmail(event.email(), event.resetLink());
        } catch (RuntimeException ex) {
            log.warn("Password reset email delivery failed for {}", event.email(), ex);
        }
    }
}
