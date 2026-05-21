package com.asaf.appointment_service.dto.account;

public record TwoFactorStatusResponse(
    boolean enabled,
    String preferredMethod
) {}
