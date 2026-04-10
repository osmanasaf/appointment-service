package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.InviteStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record EmployeeInviteResponse(
    Long id,
    Long employeeId,
    String employeeName,
    String email,
    String token,
    InviteStatus status,
    Instant expiresAt,
    Instant usedAt,
    LocalDateTime createdAt
) {}
