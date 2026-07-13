package com.asaf.appointment_service.dto.payment;

import java.math.BigDecimal;

public record InitiatePaymentResponse(
    String iframeToken,
    String merchantOid,
    BigDecimal amount,
    String currency,
    String iframeUrl
) {}
