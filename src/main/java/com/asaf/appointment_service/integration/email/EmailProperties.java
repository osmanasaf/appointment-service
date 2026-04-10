package com.asaf.appointment_service.integration.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
    String from,
    String baseUrl,
    Resend resend
) {
    public record Resend(String apiKey) {}
}
