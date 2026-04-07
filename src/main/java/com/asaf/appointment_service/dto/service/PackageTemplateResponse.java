package com.asaf.appointment_service.dto.service;

import java.math.BigDecimal;

public record PackageTemplateResponse(
    Long id,
    Long businessId,
    Long serviceId,
    String name,
    int totalSessions,
    BigDecimal price,
    boolean active
) {}
