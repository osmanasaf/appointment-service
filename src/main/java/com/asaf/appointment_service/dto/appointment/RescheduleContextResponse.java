package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import java.time.LocalDateTime;

public record RescheduleContextResponse(
    Long appointmentId,
    String businessSlug,
    String businessName,
    Long serviceId,
    String serviceName,
    Long employeeId,
    String employeeName,
    LocalDateTime currentScheduledAt,
    int durationMinutes,
    AppointmentStatus status
) {
}
