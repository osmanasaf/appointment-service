package com.asaf.appointment_service.entity.user;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.security.SecureRandom;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.BUSINESS_OWNER;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 64)
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private Instant verificationTokenExpiry;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private Instant otpExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type")
    private OtpType otpType;

    @Column(name = "otp_attempts")
    private int otpAttempts = 0;

    @Column(name = "otp_blocked_until")
    private Instant otpBlockedUntil;

    @Column(name = "password_reset_token", length = 64)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private Instant passwordResetTokenExpiry;

    protected User() {}

    private User(Builder builder) {
        this.email = builder.email;
        this.password = builder.password;
        this.name = builder.name;
        this.phoneNumber = builder.phoneNumber;
        this.businessId = builder.businessId;
        this.employeeId = builder.employeeId;
        this.role = builder.role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateProfile(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public String initiateEmailVerification() {
        String token = UUID.randomUUID().toString().replace("-", "");
        this.verificationToken = token;
        this.verificationTokenExpiry = Instant.now().plus(24, ChronoUnit.HOURS);
        return token;
    }

    public void confirmEmailVerification() {
        this.emailVerified = true;
        this.verificationToken = null;
        this.verificationTokenExpiry = null;
    }

    public boolean isVerificationTokenExpired() {
        return verificationTokenExpiry == null || Instant.now().isAfter(verificationTokenExpiry);
    }

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_BLOCK_MINUTES = 15;

    public String generateOtp(OtpType type, int validMinutes) {
        if (isOtpBlocked()) {
            throw new IllegalStateException("OTP blocked until " + otpBlockedUntil);
        }
        SecureRandom random = new SecureRandom();
        String code = String.format("%06d", random.nextInt(1000000));
        this.otpCode = code;
        this.otpExpiry = Instant.now().plus(validMinutes, ChronoUnit.MINUTES);
        this.otpType = type;
        return code;
    }

    public boolean verifyOtp(String code, OtpType expectedType) {
        if (isOtpBlocked()) {
            return false;
        }
        if (otpCode == null || otpExpiry == null || otpType != expectedType) {
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
            clearOtp();
            return true;
        }
        return false;
    }

    public void clearOtp() {
        this.otpCode = null;
        this.otpExpiry = null;
        this.otpType = null;
        this.otpAttempts = 0;
        this.otpBlockedUntil = null;
    }

    public boolean isOtpExpired() {
        return otpExpiry == null || Instant.now().isAfter(otpExpiry);
    }

    public boolean isOtpBlocked() {
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

    public Instant getOtpBlockedUntil() {
        return otpBlockedUntil;
    }

    public String generatePasswordResetToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        this.passwordResetToken = token;
        this.passwordResetTokenExpiry = Instant.now().plus(15, ChronoUnit.MINUTES);
        return token;
    }

    public boolean verifyPasswordResetToken(String token) {
        if (passwordResetToken == null || passwordResetTokenExpiry == null) {
            return false;
        }
        if (Instant.now().isAfter(passwordResetTokenExpiry)) {
            return false;
        }
        return passwordResetToken.equals(token);
    }

    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpiry = null;
    }

    public void linkToBusiness(Long businessId) {
        this.businessId = businessId;
    }

    public void linkToEmployee(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public Long getBusinessId() { return businessId; }
    public Long getEmployeeId() { return employeeId; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getVerificationToken() { return verificationToken; }

    public static class Builder {
        private String email;
        private String password;
        private String name;
        private String phoneNumber;
        private Long businessId;
        private Long employeeId;
        private UserRole role = UserRole.BUSINESS_OWNER;

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder role(UserRole role) {
            this.role = role;
            return this;
        }

        public User build() {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email zorunludur");
            }
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Şifre zorunludur");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("İsim zorunludur");
            }
            return new User(this);
        }
    }
}
