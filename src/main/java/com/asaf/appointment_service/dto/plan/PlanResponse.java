package com.asaf.appointment_service.dto.plan;

import com.asaf.appointment_service.entity.plan.PlanCode;
import java.math.BigDecimal;
import java.util.List;

public record PlanResponse(
    Long id,
    PlanCode code,
    int maxEmployees,
    int maxAppointmentsMonthly,
    int maxServices,
    int maxBranches,
    List<String> features,
    BigDecimal monthlyPrice,
    BigDecimal yearlyPrice
) {}
