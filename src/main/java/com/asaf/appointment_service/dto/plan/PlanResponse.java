package com.asaf.appointment_service.dto.plan;

import com.asaf.appointment_service.entity.plan.PlanCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PlanResponse(
    Long id,
    PlanCode code,
    int maxEmployees,
    int maxAppointmentsMonthly,
    int maxServices,
    int maxBranches,
    List<String> features,
    Map<String, String> featureNames,
    BigDecimal monthlyPrice,
    BigDecimal yearlyPrice
) {}
