package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.EmployeeStatus;
import java.time.LocalDateTime;

public record EmployeeResponse(
    Long id,
    Long businessId,
    String name,
    String phoneNumber,
    String email,
    String bio,
    String profileImageUrl,
    EmployeeStatus status,
    boolean owner,
    LocalDateTime createdAt
) {}
