package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.NotNull;

public record OffboardRequest(
    @NotNull(message = "İşlem türü zorunludur")
    OffboardAction action,

    Long newEmployeeId
) {}
