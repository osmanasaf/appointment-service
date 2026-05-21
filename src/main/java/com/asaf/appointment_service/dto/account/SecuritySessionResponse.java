package com.asaf.appointment_service.dto.account;

import java.time.Instant;

public record SecuritySessionResponse(
    Long id,
    String deviceInfo,
    Instant createdAt,
    Instant expiresAt,
    boolean revoked,
    boolean current
) {}
