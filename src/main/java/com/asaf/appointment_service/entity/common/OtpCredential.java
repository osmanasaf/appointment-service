package com.asaf.appointment_service.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Embeddable
public class OtpCredential {

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_BLOCK_MINUTES = 15;
    public static final int DEFAULT_VALID_MINUTES = 5;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private Instant otpExpiry;

    @Column(name = "otp_attempts")
    private int otpAttempts;

    @Column(name = "otp_blocked_until")
    private Instant otpBlockedUntil;

    public OtpCredential() {}

    public String generate(int validMinutes) {
        if (isBlocked()) {
            throw new IllegalStateException("OTP blocked until " + otpBlockedUntil);
        }
        SecureRandom random = new SecureRandom();
        String code = String.format("%06d", random.nextInt(1_000_000));
        this.otpCode = code;
        this.otpExpiry = Instant.now().plus(validMinutes, ChronoUnit.MINUTES);
        this.otpAttempts = 0;
        this.otpBlockedUntil = null;
        return code;
    }

    public boolean verify(String code) {
        if (isBlocked()) {
            return false;
        }
        if (otpCode == null || otpExpiry == null) {
            return false;
        }
        if (Instant.now().isAfter(otpExpiry)) {
            return false;
        }
        otpAttempts++;
        if (otpAttempts >= MAX_OTP_ATTEMPTS) {
            otpBlockedUntil = Instant.now().plus(OTP_BLOCK_MINUTES, ChronoUnit.MINUTES);
        }
        if (otpCode.equals(code)) {
            clear();
            return true;
        }
        return false;
    }

    public void clear() {
        this.otpCode = null;
        this.otpExpiry = null;
        this.otpAttempts = 0;
        this.otpBlockedUntil = null;
    }

    public boolean isBlocked() {
        if (otpBlockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(otpBlockedUntil)) {
            otpBlockedUntil = null;
            otpAttempts = 0;
            return false;
        }
        return true;
    }

    public Instant getBlockedUntil() {
        return otpBlockedUntil;
    }

    public boolean hasActiveCode() {
        return otpCode != null && otpExpiry != null && Instant.now().isBefore(otpExpiry);
    }
}
