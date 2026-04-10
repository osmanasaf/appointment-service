package com.asaf.appointment_service.integration.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
    boolean enabled,
    String provider,
    Meta meta
) {
    public record Meta(
        String phoneNumberId,
        String accessToken,
        String wabaId,
        String appSecret,
        String webhookVerifyToken,
        String apiVersion
    ) {
        public String apiVersion() {
            return apiVersion != null ? apiVersion : "v20.0";
        }
    }
    
    public String provider() {
        return provider != null ? provider : "mock";
    }
}
