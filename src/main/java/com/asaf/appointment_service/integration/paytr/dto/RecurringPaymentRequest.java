package com.asaf.appointment_service.integration.paytr.dto;

public record RecurringPaymentRequest(
    String merchantOid,
    String email,
    int paymentAmount,
    String currency,
    String userIp,
    String userName,
    String userAddress,
    String userPhone,
    String merchantOkUrl,
    String merchantFailUrl,
    String utoken,
    String ctoken,
    int testMode
) {}
