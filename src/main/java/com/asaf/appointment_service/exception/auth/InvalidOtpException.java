package com.asaf.appointment_service.exception.auth;

public class InvalidOtpException extends RuntimeException {
    
    public InvalidOtpException() {
        super("Geçersiz veya süresi dolmuş doğrulama kodu");
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}
