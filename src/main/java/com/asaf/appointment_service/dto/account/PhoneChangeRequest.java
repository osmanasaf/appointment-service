package com.asaf.appointment_service.dto.account;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneChangeRequest(
    @NotBlank(message = "Telefon zorunludur")
    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX, message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber
) {
    public record Confirm(
        @NotBlank(message = "Doğrulama kodu zorunludur")
        @Pattern(regexp = ValidationPatterns.OTP_REGEX, message = "Doğrulama kodu 6 haneli olmalıdır")
        String otp
    ) {}
}
