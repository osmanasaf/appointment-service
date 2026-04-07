package com.asaf.appointment_service.integration.sms;

public record SmsSendResult(boolean success, String messageId, String errorMessage) {

    public static SmsSendResult success(String messageId) {
        return new SmsSendResult(true, messageId, null);
    }

    public static SmsSendResult failure(String errorMessage) {
        return new SmsSendResult(false, null, errorMessage);
    }
}
