package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

public record AppointmentConfirmed(
    Long appointmentId,
    Long businessId,
    Long customerId,
    LocalDateTime scheduledAt,
    LocalDateTime confirmedAt
) {}
