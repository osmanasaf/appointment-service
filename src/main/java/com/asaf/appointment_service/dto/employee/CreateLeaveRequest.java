package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateLeaveRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    @NotNull LeaveType leaveType,
    String reason
) {}
