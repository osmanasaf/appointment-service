package com.asaf.appointment_service.dto.plan;

import com.asaf.appointment_service.entity.plan.PlanCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PublicPlanResponse(
    PlanCode code,
    BigDecimal monthlyPrice,
    BigDecimal yearlyPrice,
    int maxEmployees,
    int maxAppointmentsMonthly,
    int maxServices,
    int maxBranches,
    int maxWhatsappMonthly,
    int maxSmsMonthly,
    boolean unlimitedEmployees,
    boolean unlimitedAppointments,
    boolean unlimitedServices,
    boolean unlimitedBranches,
    boolean unlimitedWhatsapp,
    boolean unlimitedSms,
    List<String> features,
    Map<String, String> featureNames
) {}
