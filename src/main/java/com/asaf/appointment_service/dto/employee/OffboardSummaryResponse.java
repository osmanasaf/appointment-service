package com.asaf.appointment_service.dto.employee;

public record OffboardSummaryResponse(
    int affectedAppointmentCount,
    OffboardAction action,
    Long reassignedToEmployeeId
) {}
