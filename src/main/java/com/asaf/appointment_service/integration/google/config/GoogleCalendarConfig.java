package com.asaf.appointment_service.integration.google.config;

import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.GoogleCalendarHttpClient;
import com.asaf.appointment_service.integration.google.MockGoogleCalendarClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleCalendarProperties.class)
public class GoogleCalendarConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarConfig.class);

    @Bean
    public GoogleCalendarClient googleCalendarClient(GoogleCalendarProperties properties, ObjectMapper objectMapper) {
        boolean isMock = !properties.enabled()
            || properties.clientId() == null || properties.clientId().isBlank()
            || properties.clientSecret() == null || properties.clientSecret().isBlank();

        if (isMock) {
            log.info("Google Calendar entegrasyonu devre dışı/eksik, MockGoogleCalendarClient kullanılıyor");
            return new MockGoogleCalendarClient();
        }

        log.info("Google Calendar entegrasyonu aktif, GoogleCalendarHttpClient kullanılıyor");
        return new GoogleCalendarHttpClient(properties, objectMapper);
    }
}
