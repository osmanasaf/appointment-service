package com.asaf.appointment_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli bir email giriniz")
    String email,

    @NotBlank(message = "Şifre zorunludur")
    String password
) {}
