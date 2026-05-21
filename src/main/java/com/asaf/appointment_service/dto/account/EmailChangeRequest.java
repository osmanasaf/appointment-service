package com.asaf.appointment_service.dto.account;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(
    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email
) {
    public record Confirm(
        @NotBlank(message = "Doğrulama kodu zorunludur")
        @Pattern(regexp = ValidationPatterns.OTP_REGEX, message = "Doğrulama kodu 6 haneli olmalıdır")
        String otp
    ) {}
}
