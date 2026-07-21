package com.asaf.appointment_service.integration.paytr;

import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PayTR HMAC-SHA256 token hesaplama servisi.
 *
 * Direkt API token hash (iFrame ve kayıtlı kart ile tekrarlayan ödemede aynı formül —
 * bkz. dev.paytr.com/direkt-api/kart-saklama-api/kayitli-kart-tekrarlayan-odeme):
 * BASE64(HMAC-SHA256(
 *   merchant_id + user_ip + merchant_oid + email + payment_amount +
 *   payment_type + installment_count + currency + test_mode + non_3d + merchant_salt,
 *   merchant_key
 * ))
 * utoken/ctoken hash'e dahil edilmez, sadece ayrı form alanı olarak gönderilir.
 *
 * Webhook callback hash doğrulama: BASE64(HMAC-SHA256(
 *   merchant_oid + merchant_salt + status + total_amount,
 *   merchant_key
 * ))
 */
@Service
public class PayTRHashService {

    private static final Logger log = LoggerFactory.getLogger(PayTRHashService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PayTRProperties properties;

    public PayTRHashService(PayTRProperties properties) {
        this.properties = properties;
    }

    /**
     * iFrame token almak için gönderilecek paytr_token hash'ini hesaplar.
     */
    public String computeIFrameHash(String userIp, String merchantOid, String email,
                                    int paymentAmount, String paymentType,
                                    int installmentCount, String currency,
                                    int testMode, int non3d) {
        String hashInput = properties.merchantId()
                + userIp
                + merchantOid
                + email
                + paymentAmount
                + paymentType
                + installmentCount
                + currency
                + testMode
                + non3d
                + properties.merchantSalt();
        return computeHmacBase64(hashInput, properties.merchantKey());
    }

    /**
     * PayTR webhook callback'inden gelen hash'i doğrular.
     * PayTR callback body: merchant_oid, status, total_amount, hash
     * Beklenen hash: BASE64(HMAC-SHA256(merchant_oid + merchant_salt + status + total_amount, merchant_key))
     */
    public boolean verifyCallbackHash(String merchantOid, String status,
                                      String totalAmount, String receivedHash) {
        String hashInput = merchantOid + properties.merchantSalt() + status + totalAmount;
        String expectedHash = computeHmacBase64(hashInput, properties.merchantKey());
        boolean valid = expectedHash.equals(receivedHash);
        if (!valid) {
            log.warn("PayTR callback hash doğrulaması başarısız: merchantOid={}", merchantOid);
        }
        return valid;
    }

    String computeHmacBase64ForTest(String data, String key) {
        return computeHmacBase64(data, key);
    }

    private String computeHmacBase64(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("PayTR hash hesaplama başarısız", e);
        }
    }
}
