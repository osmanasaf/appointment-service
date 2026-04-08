package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.NotNull;

public record ResolveLeaveConflictRequest(
    @NotNull LeaveConflictAction action,
    Long newEmployeeId
) {}
