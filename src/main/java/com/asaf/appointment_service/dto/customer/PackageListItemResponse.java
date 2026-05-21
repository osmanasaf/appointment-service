package com.asaf.appointment_service.dto.customer;

import java.time.LocalDateTime;

public record PackageListItemResponse(
    Long id,
    Long customerId,
    String customerName,
    Long serviceId,
    Long templateId,
    String name,
    int totalSessions,
    int remainingSessions,
    LocalDateTime expiresAt,
    boolean active,
    boolean expired,
    boolean lowOnSessions
) {}
