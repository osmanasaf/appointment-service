package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

public record AppointmentCreated(
    Long appointmentId,
    Long businessId,
    Long customerId,
    Long employeeId,
    Long serviceId,
    LocalDateTime scheduledAt,
    LocalDateTime endAt,
    String status
) {}
