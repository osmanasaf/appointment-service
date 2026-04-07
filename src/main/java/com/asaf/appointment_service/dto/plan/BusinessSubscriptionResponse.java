package com.asaf.appointment_service.dto.plan;

import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import java.time.LocalDate;

public record BusinessSubscriptionResponse(
    Long businessId,
    PlanCode planCode,
    SubscriptionStatus status,
    LocalDate trialEndDate,
    LocalDate currentPeriodStart,
    LocalDate currentPeriodEnd,
    boolean manual
) {}
