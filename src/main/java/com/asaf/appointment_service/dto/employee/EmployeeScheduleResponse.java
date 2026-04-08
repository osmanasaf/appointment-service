package com.asaf.appointment_service.dto.employee;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record EmployeeScheduleResponse(
    Long id,
    Long employeeId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    LocalTime breakStart,
    LocalTime breakEnd,
    boolean closed
) {}
