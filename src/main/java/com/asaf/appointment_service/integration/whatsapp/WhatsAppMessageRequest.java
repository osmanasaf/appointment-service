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

    public static WhatsAppMessageRequest rescheduled(String phoneNumber, String customerName,
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
        return new WhatsAppMessageRequest(phoneNumber, "appointment_rescheduled", params);
    }

    public static WhatsAppMessageRequest waitlistSlotOffer(String phoneNumber, String customerName,
                                                            String serviceName, String date, String time,
                                                            String offerLink, String priceText) {
        Map<String, String> params = new HashMap<>(Map.of(
            "customer_name", customerName,
            "service_name", serviceName,
            "date", date,
            "time", time,
            "offer_link", offerLink
        ));
        if (priceText != null && !priceText.isBlank()) {
            params.put("price", priceText);
        }
        return new WhatsAppMessageRequest(phoneNumber, "waitlist_slot_offer", params);
    }

    public static WhatsAppMessageRequest reviewRequest(String phoneNumber, String customerName,
                                                        String serviceName, String reviewLink) {
        Map<String, String> params = Map.of(
            "customer_name", customerName,
            "service_name", serviceName,
            "review_link", reviewLink
        );
        return new WhatsAppMessageRequest(phoneNumber, "review_request", params);
    }

    public static WhatsAppMessageRequest otpVerification(String phoneNumber, String code) {
        return new WhatsAppMessageRequest(phoneNumber, "otp_verification", Map.of("code", code));
    }

    public static WhatsAppMessageRequest subscriptionRenewalReminder(String phoneNumber, String businessName,
                                                                      String planName, String daysBefore,
                                                                      String endDate, String renewLink) {
        Map<String, String> params = Map.of(
            "business_name", businessName,
            "plan_name", planName,
            "days_before", daysBefore,
            "end_date", endDate,
            "renew_link", renewLink
        );
        return new WhatsAppMessageRequest(phoneNumber, "subscription_renewal_reminder", params);
    }

    public static WhatsAppMessageRequest subscriptionExpired(String phoneNumber, String businessName,
                                                              String renewLink) {
        Map<String, String> params = Map.of(
            "business_name", businessName,
            "renew_link", renewLink
        );
        return new WhatsAppMessageRequest(phoneNumber, "subscription_expired", params);
    }
}
