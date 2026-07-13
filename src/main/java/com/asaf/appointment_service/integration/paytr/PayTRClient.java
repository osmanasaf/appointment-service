package com.asaf.appointment_service.integration.paytr;

import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenRequest;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenResponse;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentRequest;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentResponse;

public interface PayTRClient {

    IFrameTokenResponse getIFrameToken(IFrameTokenRequest request);

    RecurringPaymentResponse chargeRecurring(RecurringPaymentRequest request);
}
