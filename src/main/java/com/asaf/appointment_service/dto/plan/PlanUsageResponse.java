package com.asaf.appointment_service.dto.plan;

public record PlanUsageResponse(
    int year,
    int month,
    int appointmentsCount,
    int smsSent,
    int whatsappSent,
    int maxAppointmentsMonthly
) {}
