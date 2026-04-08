package com.asaf.appointment_service.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ResolveAndCreateLeaveRequest(
    @NotNull @Valid CreateLeaveRequest leave,
    @NotNull @Valid ResolveLeaveConflictRequest resolve
) {}
