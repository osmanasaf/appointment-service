package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails;
import com.asaf.appointment_service.service.notification.WhatsAppWebhookService;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Value("${app.whatsapp.webhook.verify-token:}")
    private String expectedVerifyToken;

    private final WhatsAppWebhookService webhookService;

    public WebhookController(WhatsAppWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<Object> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && expectedVerifyToken != null && !expectedVerifyToken.isBlank()
                && expectedVerifyToken.equals(verifyToken)) {
            log.info("WhatsApp webhook doğrulandı");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook doğrulaması başarısız");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(new ErrorDetails("WEBHOOK_VERIFICATION_FAILED", "Verification failed", null, LocalDateTime.now())));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<ApiResponse<Void>> handleWhatsAppWebhook(@RequestBody Map<String, Object> payload) {
        log.debug("WhatsApp webhook alındı");
        webhookService.processWebhookPayload(payload);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed"));
    }
}
