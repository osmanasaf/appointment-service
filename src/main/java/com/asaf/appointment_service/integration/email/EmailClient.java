package com.asaf.appointment_service.integration.email;

public interface EmailClient {

    void sendVerificationEmail(String toEmail, String toName, String verificationToken);
    
    void sendEmployeeInvite(String toEmail, String employeeName, String businessName, String inviteToken);

    void sendOtpEmail(String toEmail, String toName, String otpCode, OtpEmailType type);

    enum OtpEmailType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        EMAIL_CHANGE,
        PHONE_CHANGE
    }
}
