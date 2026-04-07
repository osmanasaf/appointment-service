package com.asaf.appointment_service.dto.plan;

import com.asaf.appointment_service.entity.plan.PlanCode;
import jakarta.validation.constraints.NotNull;

public record AdminAssignPlanRequest(
    @NotNull(message = "Plan kodu zorunludur")
    PlanCode planCode
) {}
