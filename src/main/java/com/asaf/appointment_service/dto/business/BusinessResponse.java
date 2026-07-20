package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.business.BusinessStatus;
import java.time.LocalDateTime;

public record BusinessResponse(
    Long id,
    String name,
    String slug,
    String phoneNumber,
    String email,
    String address,
    String description,
    String logoUrl,
    BusinessCategory category,
    String categoryLabel,
    boolean autoConfirm,
    int reminderHoursBefore,
    boolean sameDayBookingAllowed,
    int maxAdvanceBookingDays,
    Integer maxAppointmentsPerCustomerPerDay,
    boolean whatsappSendPrice,
    BusinessStatus status,
    LocalDateTime createdAt,
    Double averageRating,
    int reviewCount
) {}
