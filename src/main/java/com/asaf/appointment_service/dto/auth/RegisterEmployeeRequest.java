package com.asaf.appointment_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterEmployeeRequest(
    @NotBlank(message = "E-posta gereklidir")
    @Email(message = "Geçerli bir e-posta giriniz")
    String email,

    @NotBlank(message = "Şifre gereklidir")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    String password,

    @NotBlank(message = "Ad soyad gereklidir")
    @Size(min = 2, message = "Ad soyad en az 2 karakter olmalıdır")
    String name,

    String phoneNumber,

    @NotBlank(message = "Davet kodu gereklidir")
    String inviteToken
) {}
