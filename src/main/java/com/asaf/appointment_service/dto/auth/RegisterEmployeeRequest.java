package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterEmployeeRequest(
    @NotBlank(message = "E-posta gereklidir")
    @Email(message = "Geçerli bir e-posta giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @NotBlank(message = "Şifre gereklidir")
    @Pattern(regexp = ValidationPatterns.PASSWORD_REGEX, message = ValidationPatterns.PASSWORD_MESSAGE)
    String password,

    @NotBlank(message = "Ad soyad gereklidir")
    @Size(min = 2, max = 100, message = "Ad soyad 2-100 karakter olmalıdır")
    String name,

    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    @NotBlank(message = "Davet kodu gereklidir")
    String inviteToken
) {
    public RegisterEmployeeRequest {
        if (phoneNumber != null && phoneNumber.isBlank()) {
            phoneNumber = null;
        }
    }
}
