package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli bir email giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @NotBlank(message = "Şifre zorunludur")
    @Size(max = ValidationPatterns.PASSWORD_MAX_LENGTH, message = "Şifre çok uzun")
    String password
) {}
