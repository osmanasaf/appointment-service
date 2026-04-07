package com.asaf.appointment_service.mapper.appointment;

import com.asaf.appointment_service.dto.appointment.AppointmentResponse;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;

public class AppointmentMapper {

    private AppointmentMapper() {}

    public static Appointment toEntity(CreateAppointmentRequest request, int durationMinutes) {
        return Appointment.builder()
            .businessId(request.businessId())
            .customerId(request.customerId())
            .employeeId(request.employeeId())
            .serviceId(request.serviceId())
            .timeSlot(request.scheduledAt(), durationMinutes)
            .notes(request.notes())
            .customerNotes(request.customerNotes())
            .source(request.getSource())
            .build();
    }

    public static AppointmentResponse toResponse(Appointment entity) {
        return new AppointmentResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getCustomerId(),
            entity.getEmployeeId(),
            entity.getServiceId(),
            entity.getScheduledAt(),
            entity.getEndAt(),
            entity.getDurationMinutes(),
            entity.getStatus(),
            entity.isRisky(),
            entity.getRiskReason(),
            entity.getConfirmedAt(),
            entity.getNotes(),
            entity.getCustomerNotes(),
            entity.isReminderSent(),
            entity.getCreatedAt(),
            entity.getSource(),
            entity.getPackageSessionId()
        );
    }
}
