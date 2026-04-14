package com.asaf.appointment_service.dto.superadmin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SuperAdminBusinessDetailResponse(
    Long businessId,
    String businessName,
    String slug,
    String category,
    String status,
    LocalDateTime registeredAt,
    String ownerName,
    String ownerEmail,
    String planCode,
    String subscriptionStatus,
    LocalDate currentPeriodStart,
    LocalDate currentPeriodEnd,
    LocalDate trialEndDate,
    boolean manual,
    int maxEmployees,
    int maxAppointmentsMonthly,
    int maxServices,
    int maxWhatsappMonthly,
    int maxSmsMonthly,
    int currentEmployeeCount,
    int currentMonthAppointments,
    int currentMonthSmsSent,
    int currentMonthWhatsappSent,
    int currentServiceCount,
    double employeeUsageRate,
    double appointmentUsageRate,
    double serviceUsageRate
) {}
