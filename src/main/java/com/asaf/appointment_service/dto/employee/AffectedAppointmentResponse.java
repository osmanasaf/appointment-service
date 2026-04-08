package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import java.time.LocalDateTime;

public record AffectedAppointmentResponse(
    Long id,
    Long customerId,
    Long serviceId,
    LocalDateTime scheduledAt,
    AppointmentStatus status
) {}
