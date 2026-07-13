package com.asaf.appointment_service.dto.payment;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.payment.PaymentTransactionStatus;
import com.asaf.appointment_service.entity.payment.PaymentType;
import com.asaf.appointment_service.entity.plan.PlanCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
    Long id,
    String merchantOid,
    BigDecimal amount,
    String currency,
    PaymentTransactionStatus status,
    PaymentType paymentType,
    PlanCode planCode,
    BillingPeriod billingPeriod,
    LocalDateTime createdAt
) {}
