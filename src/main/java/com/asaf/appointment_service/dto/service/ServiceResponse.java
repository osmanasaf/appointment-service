package com.asaf.appointment_service.dto.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
    Long id,
    Long businessId,
    String name,
    String description,
    int durationMinutes,
    int bufferBeforeMinutes,
    int bufferAfterMinutes,
    int totalDurationMinutes,
    BigDecimal price,
    String currency,
    boolean requiresConfirmation,
    boolean sameDayBookingAllowed,
    boolean packageEligible,
    boolean active,
    int displayOrder,
    LocalDateTime createdAt
) {}
