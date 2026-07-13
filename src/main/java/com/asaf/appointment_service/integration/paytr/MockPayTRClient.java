package com.asaf.appointment_service.integration.paytr;

import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenRequest;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenResponse;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentRequest;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockPayTRClient implements PayTRClient {

    private static final Logger log = LoggerFactory.getLogger(MockPayTRClient.class);
    private static final String MOCK_TOKEN = "MOCK_PAYTR_TOKEN_FOR_TESTING";

    @Override
    public IFrameTokenResponse getIFrameToken(IFrameTokenRequest request) {
        log.info("[MOCK] PayTR iFrame token istendi: merchantOid={}, amount={}",
                request.merchantOid(), request.paymentAmount());
        return IFrameTokenResponse.success(MOCK_TOKEN);
    }

    @Override
    public RecurringPaymentResponse chargeRecurring(RecurringPaymentRequest request) {
        log.info("[MOCK] PayTR recurring ödeme tetiklendi: merchantOid={}, amount={}",
                request.merchantOid(), request.paymentAmount());
        return RecurringPaymentResponse.ofSuccess();
    }
}
