package com.asaf.appointment_service.integration.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MockWhatsAppClient implements WhatsAppClientInterface {

    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppClient.class);

    @Override
    public WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request) {
        log.info("[MOCK] WhatsApp template mesajı: to={}, template={}, params={}", 
            maskPhone(request.to()), request.templateName(), request.parameters());
        return WhatsAppMessageResponse.success("mock-" + System.currentTimeMillis());
    }

    @Override
    public WhatsAppMessageResponse sendTextMessage(String to, String text) {
        log.info("[MOCK] WhatsApp text mesajı: to={}, text={}", maskPhone(to), text);
        return WhatsAppMessageResponse.success("mock-text-" + System.currentTimeMillis());
    }

    @Override
    public WhatsAppMessageResponse sendOtp(String to, String code) {
        log.info("[MOCK] WhatsApp OTP: to={}, code={}", maskPhone(to), code);
        return WhatsAppMessageResponse.success("mock-otp-" + System.currentTimeMillis());
    }

    @Override
    public WhatsAppMessageResponse sendInteractiveButtons(String to, String header, String body, String footer, List<ButtonOption> buttons) {
        log.info("[MOCK] WhatsApp interactive mesajı: to={}, header={}, buttons={}", 
            maskPhone(to), header, buttons.stream().map(ButtonOption::title).toList());
        return WhatsAppMessageResponse.success("mock-interactive-" + System.currentTimeMillis());
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
