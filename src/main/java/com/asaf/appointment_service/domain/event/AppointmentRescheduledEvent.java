package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

public record AppointmentRescheduledEvent(
    Long appointmentId,
    Long businessId,
    Long oldEmployeeId,
    Long newEmployeeId,
    LocalDateTime newScheduledAt,
    LocalDateTime newEndAt
) {}
