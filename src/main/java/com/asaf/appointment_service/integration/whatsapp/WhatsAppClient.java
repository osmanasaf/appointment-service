package com.asaf.appointment_service.integration.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WhatsApp Business API Client.
 * 
 * MVP: Mock implementation - gerçek API entegrasyonu için
 * Meta WhatsApp Business API veya Twilio kullanılabilir.
 */
@Component
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);

    @Value("${whatsapp.api.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api.access-token:}")
    private String accessToken;

    public WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request) {
        if (!enabled) {
            log.info("[MOCK] WhatsApp mesajı gönderildi: to={}, template={}", 
                request.to(), request.templateName());
            return WhatsAppMessageResponse.success("mock-" + System.currentTimeMillis());
        }

        try {
            String messageId = sendToWhatsAppApi(request);
            log.info("WhatsApp mesajı gönderildi: to={}, messageId={}", request.to(), messageId);
            return WhatsAppMessageResponse.success(messageId);
        } catch (Exception e) {
            log.error("WhatsApp mesajı gönderilemedi: to={}, error={}", request.to(), e.getMessage());
            return WhatsAppMessageResponse.failure(e.getMessage());
        }
    }

    private String sendToWhatsAppApi(WhatsAppMessageRequest request) {
        // TODO: Gerçek Meta WhatsApp Business API entegrasyonu
        // POST https://graph.facebook.com/v17.0/{phoneNumberId}/messages
        // Headers: Authorization: Bearer {accessToken}
        // Body: {
        //   "messaging_product": "whatsapp",
        //   "to": request.to(),
        //   "type": "template",
        //   "template": {
        //     "name": request.templateName(),
        //     "language": { "code": "tr" },
        //     "components": [...]
        //   }
        // }
        
        throw new UnsupportedOperationException("WhatsApp API entegrasyonu henüz implement edilmedi");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
