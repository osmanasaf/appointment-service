package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

public record AppointmentCompleted(
    Long appointmentId,
    Long businessId,
    Long customerId,
    Long serviceId,
    LocalDateTime scheduledAt,
    LocalDateTime completedAt
) {}
