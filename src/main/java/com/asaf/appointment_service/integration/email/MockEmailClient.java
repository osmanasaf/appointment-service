package com.asaf.appointment_service.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class MockEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(MockEmailClient.class);

    private final EmailProperties emailProperties;

    public MockEmailClient(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String verificationToken) {
        String verificationUrl = emailProperties.baseUrl() + "/auth/verify-email?token=" + verificationToken;
        log.info("[MOCK EMAIL] Doğrulama emaili → to={} name={} url={}", toEmail, toName, verificationUrl);
    }
}
