package com.asaf.appointment_service.security.ratelimit;

public enum RateLimitType {
    AUTH,
    PUBLIC_API,
    WEBHOOK,
    INVITE
}
