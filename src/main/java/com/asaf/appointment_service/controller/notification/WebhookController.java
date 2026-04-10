package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails;
import com.asaf.appointment_service.integration.whatsapp.config.WhatsAppProperties;
import com.asaf.appointment_service.security.webhook.WebhookSignatureVerifier;
import com.asaf.appointment_service.service.notification.WhatsAppWebhookService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WhatsAppWebhookService webhookService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final WhatsAppProperties whatsAppProperties;
    private final ObjectMapper objectMapper;

    public WebhookController(
            WhatsAppWebhookService webhookService,
            WebhookSignatureVerifier signatureVerifier,
            WhatsAppProperties whatsAppProperties,
            ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.signatureVerifier = signatureVerifier;
        this.whatsAppProperties = whatsAppProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<Object> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        
        String expectedToken = whatsAppProperties.meta().webhookVerifyToken();
        
        if ("subscribe".equals(mode) && expectedToken != null && !expectedToken.isBlank()
                && expectedToken.equals(verifyToken)) {
            log.info("WhatsApp webhook doğrulandı");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook doğrulaması başarısız: mode={}, tokenMatch={}", 
            mode, expectedToken != null && expectedToken.equals(verifyToken));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(new ErrorDetails("WEBHOOK_VERIFICATION_FAILED", "Verification failed", null, LocalDateTime.now())));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<ApiResponse<Void>> handleWhatsAppWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] rawPayload) {
        
        if (signatureVerifier.isVerificationEnabled()) {
            if (!signatureVerifier.verifySignature(signature, rawPayload)) {
                log.warn("WhatsApp webhook imza doğrulaması başarısız");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(new ErrorDetails(
                        "INVALID_SIGNATURE", 
                        "Webhook signature verification failed", 
                        null, 
                        LocalDateTime.now())));
            }
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {});
            log.debug("WhatsApp webhook alındı");
            webhookService.processWebhookPayload(payload);
            return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed"));
        } catch (Exception e) {
            log.error("Webhook payload parse hatası: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(new ErrorDetails(
                    "INVALID_PAYLOAD", 
                    "Failed to parse webhook payload", 
                    null, 
                    LocalDateTime.now())));
        }
    }
}
