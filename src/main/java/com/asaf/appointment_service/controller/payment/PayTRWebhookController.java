package com.asaf.appointment_service.controller.payment;

import com.asaf.appointment_service.exception.payment.PaymentException;
import com.asaf.appointment_service.service.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PayTR webhook callback endpoint'i.
 *
 * PayTR, ödeme sonucunu form-urlencoded POST ile bu adrese gönderir.
 * PayTR, yanıt olarak düz metin "OK" bekler — JSON DEĞİL.
 * Başka bir yanıt gelirse callback'i tekrar tekrar gönderir.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class PayTRWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayTRWebhookController.class);
    private static final String OK_RESPONSE = "OK";

    private final PaymentService paymentService;

    public PayTRWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(value = "/paytr", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handlePayTRCallback(
            @RequestParam("merchant_oid") String merchantOid,
            @RequestParam("status") String status,
            @RequestParam("total_amount") String totalAmount,
            @RequestParam("hash") String hash,
            @RequestParam(value = "utoken", required = false) String utoken,
            @RequestParam(value = "ctoken", required = false) String ctoken,
            @RequestParam(value = "card_last4", required = false) String last4,
            @RequestParam(value = "card_brand", required = false) String cardBrand,
            @RequestParam(value = "card_expiry_month", required = false) String expiryMonth,
            @RequestParam(value = "card_expiry_year", required = false) String expiryYear) {

        log.info("PayTR webhook alındı: merchantOid={}, status={}", merchantOid, status);

        try {
            paymentService.processCallback(merchantOid, status, totalAmount, hash,
                    utoken, ctoken, last4, cardBrand, expiryMonth, expiryYear);
            log.info("PayTR webhook başarıyla işlendi: merchantOid={}", merchantOid);
        } catch (PaymentException e) {
            log.error("PayTR webhook işlenemedi: merchantOid={}, hata={}", merchantOid, e.getMessage());
        } catch (Exception e) {
            log.error("PayTR webhook beklenmeyen hata: merchantOid={}", merchantOid, e);
        }

        return ResponseEntity.ok(OK_RESPONSE);
    }
}
