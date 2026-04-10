package com.asaf.appointment_service.integration.whatsapp;

import java.util.List;

public interface WhatsAppClientInterface {
    
    WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request);
    
    WhatsAppMessageResponse sendTextMessage(String to, String text);
    
    WhatsAppMessageResponse sendInteractiveButtons(String to, String header, String body, String footer, List<ButtonOption> buttons);
    
    boolean isEnabled();
    
    record ButtonOption(String id, String title) {}
}
