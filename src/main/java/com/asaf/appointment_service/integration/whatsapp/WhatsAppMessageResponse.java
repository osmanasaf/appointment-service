package com.asaf.appointment_service.integration.whatsapp;

public record WhatsAppMessageResponse(
    String messageId,
    boolean success,
    String errorMessage
) {
    public static WhatsAppMessageResponse success(String messageId) {
        return new WhatsAppMessageResponse(messageId, true, null);
    }

    public static WhatsAppMessageResponse failure(String errorMessage) {
        return new WhatsAppMessageResponse(null, false, errorMessage);
    }
}
