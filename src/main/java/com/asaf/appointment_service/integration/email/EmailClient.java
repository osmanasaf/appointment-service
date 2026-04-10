package com.asaf.appointment_service.integration.email;

public interface EmailClient {

    void sendVerificationEmail(String toEmail, String toName, String verificationToken);
}
