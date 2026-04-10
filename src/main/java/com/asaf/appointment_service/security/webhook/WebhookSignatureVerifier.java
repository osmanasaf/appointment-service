package com.asaf.appointment_service.security.webhook;

import com.asaf.appointment_service.integration.whatsapp.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final WhatsAppProperties whatsAppProperties;

    public WebhookSignatureVerifier(WhatsAppProperties whatsAppProperties) {
        this.whatsAppProperties = whatsAppProperties;
    }

    public boolean verifySignature(String signature, byte[] payload) {
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook imza header'ı eksik");
            return false;
        }

        String appSecret = whatsAppProperties.meta().appSecret();
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("WhatsApp app secret yapılandırılmamış, imza doğrulama atlanıyor");
            return true;
        }

        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Geçersiz imza formatı: sha256= prefix'i eksik");
            return false;
        }

        String receivedHash = signature.substring(SIGNATURE_PREFIX.length());
        String computedHash = computeHmacSha256(payload, appSecret);

        if (computedHash == null) {
            log.error("HMAC hesaplaması başarısız");
            return false;
        }

        boolean valid = MessageDigest.isEqual(
            receivedHash.toLowerCase().getBytes(StandardCharsets.UTF_8),
            computedHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("Webhook imza doğrulaması başarısız");
        }

        return valid;
    }

    private String computeHmacSha256(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC hesaplama hatası: {}", e.getMessage());
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public boolean isVerificationEnabled() {
        String appSecret = whatsAppProperties.meta().appSecret();
        return appSecret != null && !appSecret.isBlank();
    }
}
