package com.asaf.appointment_service.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class MockEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(MockEmailClient.class);

    private final EmailProperties emailProperties;

    public MockEmailClient(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String verificationToken) {
        String verificationUrl = emailProperties.baseUrl() + "/auth/verify-email?token=" + verificationToken;
        log.info("[MOCK EMAIL] Doğrulama emaili → to={} name={} url={}", toEmail, toName, verificationUrl);
    }

    @Override
    public void sendEmployeeInvite(String toEmail, String employeeName, String businessName, String inviteToken) {
        String inviteUrl = emailProperties.baseUrl() + "/register-employee?token=" + inviteToken;
        log.info("[MOCK EMAIL] Çalışan daveti → to={} employee={} business={} url={}", 
            toEmail, employeeName, businessName, inviteUrl);
    }

    @Override
    public void sendOtpEmail(String toEmail, String toName, String otpCode, OtpEmailType type) {
        log.info("[MOCK EMAIL] OTP → to={} name={} code={} type={}", toEmail, toName, otpCode, type);
    }

    @Override
    public void sendSubscriptionRenewalReminder(String toEmail, String businessName, String planName,
                                                String endDate, int daysBefore, String renewUrl) {
        log.info("[MOCK EMAIL] Abonelik hatırlatma → to={} business={} plan={} endDate={} daysBefore={} url={}",
            toEmail, businessName, planName, endDate, daysBefore, renewUrl);
    }

    @Override
    public void sendSubscriptionExpired(String toEmail, String businessName, String renewUrl) {
        log.info("[MOCK EMAIL] Abonelik süresi doldu → to={} business={} url={}", toEmail, businessName, renewUrl);
    }
}
