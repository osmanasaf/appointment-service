package com.asaf.appointment_service.integration.whatsapp;

import java.util.HashMap;
import java.util.Map;

public record WhatsAppMessageRequest(
    String to,
    String templateName,
    Map<String, String> parameters
) {
    public static WhatsAppMessageRequest reminder(String phoneNumber, String customerName,
                                                   String serviceName, String date, String time,
                                                   String priceText) {
        Map<String, String> params = new HashMap<>(Map.of(
            "customer_name", customerName,
            "service_name", serviceName,
            "date", date,
            "time", time
        ));
        if (priceText != null && !priceText.isBlank()) {
            params.put("price", priceText);
        }
        return new WhatsAppMessageRequest(phoneNumber, "appointment_reminder", params);
    }

    public static WhatsAppMessageRequest confirmation(String phoneNumber, String customerName,
                                                       String serviceName, String date, String time,
                                                       String confirmLink, String priceText) {
        Map<String, String> params = new HashMap<>(Map.of(
            "customer_name", customerName,
            "service_name", serviceName,
            "date", date,
            "time", time,
            "confirm_link", confirmLink
        ));
        if (priceText != null && !priceText.isBlank()) {
            params.put("price", priceText);
        }
        return new WhatsAppMessageRequest(phoneNumber, "appointment_confirmation", params);
    }

    public static WhatsAppMessageRequest cancelled(String phoneNumber, String customerName,
                                                    String serviceName, String date, String priceText) {
        Map<String, String> params = new HashMap<>(Map.of(
            "customer_name", customerName,
            "service_name", serviceName,
            "date", date
        ));
        if (priceText != null && !priceText.isBlank()) {
            params.put("price", priceText);
        }
        return new WhatsAppMessageRequest(phoneNumber, "appointment_cancelled", params);
    }
}
