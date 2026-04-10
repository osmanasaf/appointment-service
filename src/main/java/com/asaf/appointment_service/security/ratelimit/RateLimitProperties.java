package com.asaf.appointment_service.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ratelimit")
public record RateLimitProperties(
    boolean enabled,
    AuthLimit auth,
    PublicLimit publicApi,
    WebhookLimit webhook,
    InviteLimit invite
) {
    public record AuthLimit(int requestsPerMinute) {
        public int requestsPerMinute() {
            return requestsPerMinute > 0 ? requestsPerMinute : 10;
        }
    }

    public record PublicLimit(int requestsPerMinute) {
        public int requestsPerMinute() {
            return requestsPerMinute > 0 ? requestsPerMinute : 30;
        }
    }

    public record WebhookLimit(int requestsPerMinute) {
        public int requestsPerMinute() {
            return requestsPerMinute > 0 ? requestsPerMinute : 100;
        }
    }

    public record InviteLimit(int requestsPerHour) {
        public int requestsPerHour() {
            return requestsPerHour > 0 ? requestsPerHour : 10;
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public AuthLimit auth() {
        return auth != null ? auth : new AuthLimit(10);
    }

    public PublicLimit publicApi() {
        return publicApi != null ? publicApi : new PublicLimit(30);
    }

    public WebhookLimit webhook() {
        return webhook != null ? webhook : new WebhookLimit(100);
    }

    public InviteLimit invite() {
        return invite != null ? invite : new InviteLimit(10);
    }
}
