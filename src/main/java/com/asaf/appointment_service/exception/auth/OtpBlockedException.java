package com.asaf.appointment_service.exception.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class OtpBlockedException extends RuntimeException {
    
    private final Instant blockedUntil;
    
    public OtpBlockedException() {
        super("Çok fazla hatalı deneme. Lütfen 15 dakika sonra tekrar deneyin.");
        this.blockedUntil = null;
    }
    
    public OtpBlockedException(Instant blockedUntil) {
        super(buildMessage(blockedUntil));
        this.blockedUntil = blockedUntil;
    }
    
    private static String buildMessage(Instant blockedUntil) {
        if (blockedUntil == null) {
            return "Çok fazla hatalı deneme. Lütfen 15 dakika sonra tekrar deneyin.";
        }
        long minutesRemaining = Instant.now().until(blockedUntil, ChronoUnit.MINUTES);
        if (minutesRemaining <= 0) {
            return "Çok fazla hatalı deneme. Lütfen tekrar deneyin.";
        }
        return String.format("Çok fazla hatalı deneme. %d dakika sonra tekrar deneyebilirsiniz.", minutesRemaining + 1);
    }
    
    public Instant getBlockedUntil() {
        return blockedUntil;
    }
}
