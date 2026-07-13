package com.asaf.appointment_service.integration.email;

public interface EmailClient {

    void sendVerificationEmail(String toEmail, String toName, String verificationToken);
    
    void sendEmployeeInvite(String toEmail, String employeeName, String businessName, String inviteToken);

    void sendOtpEmail(String toEmail, String toName, String otpCode, OtpEmailType type);

    void sendSubscriptionRenewalReminder(String toEmail, String businessName, String planName,
                                         String endDate, int daysBefore, String renewUrl);

    void sendSubscriptionExpired(String toEmail, String businessName, String renewUrl);

    enum OtpEmailType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        EMAIL_CHANGE,
        PHONE_CHANGE
    }
}
