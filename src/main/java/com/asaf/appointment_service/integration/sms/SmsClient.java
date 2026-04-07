package com.asaf.appointment_service.integration.sms;

public interface SmsClient {

    SmsSendResult send(String toPhoneNumber, String message);
}
