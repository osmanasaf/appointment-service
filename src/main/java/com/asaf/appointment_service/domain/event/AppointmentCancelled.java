package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

public record AppointmentCancelled(
    Long appointmentId,
    Long businessId,
    Long customerId,
    LocalDateTime scheduledAt,
    String cancellationReason
) {}
