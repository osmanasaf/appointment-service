package com.asaf.appointment_service.dto.payment;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.plan.PlanCode;
import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(
    @NotNull PlanCode planCode,
    @NotNull BillingPeriod billingPeriod,
    @NotNull String okUrl,
    @NotNull String failUrl
) {}
