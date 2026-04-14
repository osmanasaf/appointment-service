package com.asaf.appointment_service.dto.superadmin;

import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(
    @NotNull(message = "Plan kodu zorunludur")
    String planCode,
    Integer periodMonths
) {}
