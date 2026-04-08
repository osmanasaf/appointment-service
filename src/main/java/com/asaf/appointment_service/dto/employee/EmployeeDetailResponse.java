package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.EmployeeStatus;
import java.time.LocalDateTime;

public record EmployeeDetailResponse(
    Long id,
    Long businessId,
    String name,
    String title,
    String phoneNumber,
    String email,
    String bio,
    String profileImageUrl,
    EmployeeStatus status,
    boolean owner,
    Integer experienceYears,
    boolean acceptsOnlineBooking,
    LocalDateTime createdAt
) {}
