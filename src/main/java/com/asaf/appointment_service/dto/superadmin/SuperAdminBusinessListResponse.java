package com.asaf.appointment_service.dto.superadmin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SuperAdminBusinessListResponse(
    Long businessId,
    String businessName,
    String slug,
    String category,
    String ownerName,
    String ownerEmail,
    String planCode,
    String subscriptionStatus,
    LocalDate subscriptionEnd,
    int employeeCount,
    int currentMonthAppointments,
    LocalDateTime registeredAt
) {}
