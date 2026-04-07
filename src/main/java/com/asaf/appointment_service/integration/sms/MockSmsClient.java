package com.asaf.appointment_service.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsClient implements SmsClient {

    private static final Logger log = LoggerFactory.getLogger(MockSmsClient.class);

    @Override
    public SmsSendResult send(String toPhoneNumber, String message) {
        log.info("[MOCK] SMS gönderildi: to={}, length={}", toPhoneNumber != null ? "***" + (toPhoneNumber.length() > 4 ? toPhoneNumber.substring(toPhoneNumber.length() - 4) : "") : "null", message != null ? message.length() : 0);
        return SmsSendResult.success("mock-sms-" + System.currentTimeMillis());
    }
}
