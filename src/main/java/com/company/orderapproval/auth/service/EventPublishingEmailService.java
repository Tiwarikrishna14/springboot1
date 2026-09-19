package com.company.orderapproval.auth.service;

import com.company.orderapproval.auth.event.PasswordResetEmailEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EventPublishingEmailService implements EmailService {

    private final ApplicationEventPublisher eventPublisher;

    public EventPublishingEmailService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        eventPublisher.publishEvent(new PasswordResetEmailEvent(email, resetLink));
    }
}
