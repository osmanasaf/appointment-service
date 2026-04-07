package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(
    Long id,
    Long businessId,
    Long customerId,
    Long employeeId,
    Long serviceId,
    LocalDateTime scheduledAt,
    LocalDateTime endAt,
    int durationMinutes,
    AppointmentStatus status,
    boolean risky,
    String riskReason,
    LocalDateTime confirmedAt,
    String notes,
    String customerNotes,
    boolean reminderSent,
    LocalDateTime createdAt,
    AppointmentSource source,
    Long packageSessionId
) {}
