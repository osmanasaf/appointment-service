package com.asaf.appointment_service.dto.payment;

import java.time.LocalDateTime;

public record StoredCardResponse(
    Long id,
    String last4,
    String cardBrand,
    int expiryMonth,
    int expiryYear,
    boolean isDefault,
    LocalDateTime createdAt
) {}
