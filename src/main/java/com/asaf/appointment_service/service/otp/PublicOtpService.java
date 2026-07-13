package com.asaf.appointment_service.service.otp;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.common.util.PhoneNumberUtil;
import com.asaf.appointment_service.entity.common.OtpCredential;
import com.asaf.appointment_service.entity.otp.PublicOtpVerification;
import com.asaf.appointment_service.exception.auth.InvalidOtpException;
import com.asaf.appointment_service.exception.auth.OtpBlockedException;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.otp.PublicOtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicOtpService {

    private static final Logger log = LoggerFactory.getLogger(PublicOtpService.class);
    private static final int OTP_VALID_MINUTES = OtpCredential.DEFAULT_VALID_MINUTES;

    private final PublicOtpVerificationRepository repository;
    private final WhatsAppClientInterface whatsAppClient;

    public PublicOtpService(PublicOtpVerificationRepository repository,
                            WhatsAppClientInterface whatsAppClient) {
        this.repository = repository;
        this.whatsAppClient = whatsAppClient;
    }

    @Transactional
    public void requestOtp(Long businessId, String phoneCountryCode, String phoneNumber) {
        String normalizedPhone = PhoneNumberUtil.formatForWhatsApp(phoneCountryCode, phoneNumber);
        PublicOtpVerification verification = repository
            .findByBusinessIdAndPhoneNormalized(businessId, normalizedPhone)
            .orElseGet(() -> new PublicOtpVerification(businessId, normalizedPhone));

        OtpCredential credential = verification.getOtpCredential();
        if (credential.isBlocked()) {
            throw new OtpBlockedException(credential.getBlockedUntil());
        }

        String code = credential.generate(OTP_VALID_MINUTES);
        repository.save(verification);

        String whatsAppTo = PhoneNumberUtil.formatWithCountryCode(phoneCountryCode, phoneNumber);
        WhatsAppMessageResponse response = whatsAppClient.sendOtp(whatsAppTo, code);
        if (!response.success()) {
            throw new BusinessException("OTP_SEND_FAILED",
                "Doğrulama kodu gönderilemedi. Lütfen tekrar deneyin.");
        }

        log.info("Public OTP gönderildi: businessId={}, phone={}",
            businessId, PhoneNumberUtil.maskForLog(normalizedPhone));
    }

    @Transactional
    public void verifyAndConsume(Long businessId, String phoneCountryCode, String phoneNumber, String otpCode) {
        String normalizedPhone = PhoneNumberUtil.formatForWhatsApp(phoneCountryCode, phoneNumber);
        PublicOtpVerification verification = repository
            .findByBusinessIdAndPhoneNormalized(businessId, normalizedPhone)
            .orElseThrow(InvalidOtpException::new);

        OtpCredential credential = verification.getOtpCredential();
        if (credential.isBlocked()) {
            throw new OtpBlockedException(credential.getBlockedUntil());
        }

        if (!credential.verify(otpCode)) {
            repository.save(verification);
            if (credential.isBlocked()) {
                throw new OtpBlockedException(credential.getBlockedUntil());
            }
            throw new InvalidOtpException();
        }

        repository.delete(verification);
        log.info("Public OTP doğrulandı: businessId={}, phone={}",
            businessId, PhoneNumberUtil.maskForLog(normalizedPhone));
    }
}
