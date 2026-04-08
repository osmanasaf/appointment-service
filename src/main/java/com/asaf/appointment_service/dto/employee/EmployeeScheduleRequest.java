package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record EmployeeScheduleRequest(
    @NotNull(message = "Gün zorunludur")
    DayOfWeek dayOfWeek,

    LocalTime startTime,

    LocalTime endTime,

    LocalTime breakStart,

    LocalTime breakEnd,

    boolean closed
) {}
