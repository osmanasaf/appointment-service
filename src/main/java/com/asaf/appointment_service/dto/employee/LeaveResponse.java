package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.LeaveStatus;
import com.asaf.appointment_service.entity.employee.LeaveType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record LeaveResponse(
    Long id,
    Long employeeId,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    boolean fullDay,
    LeaveType leaveType,
    LeaveStatus status,
    String reason,
    LocalDateTime createdAt
) {}
