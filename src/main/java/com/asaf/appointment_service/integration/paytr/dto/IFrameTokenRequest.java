package com.asaf.appointment_service.integration.paytr.dto;

public record IFrameTokenRequest(
    String merchantOid,
    String email,
    int paymentAmount,
    String currency,
    String userIp,
    String userName,
    String userPhone,
    String userAddress,
    String merchantOkUrl,
    String merchantFailUrl,
    boolean storeCard,
    int installmentCount,
    int testMode
) {}
