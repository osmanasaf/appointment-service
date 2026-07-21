package com.asaf.appointment_service.integration.google.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.calendar")
public record GoogleCalendarProperties(
    boolean enabled,
    String clientId,
    String clientSecret,
    String redirectUri
) {}
