package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.entity.business.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli bir email giriniz")
    String email,

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    String password,

    @NotBlank(message = "İsim zorunludur")
    String name,

    String phoneNumber,

    @NotBlank(message = "İşletme adı zorunludur")
    String businessName,

    @NotNull(message = "İşletme kategorisi zorunludur")
    BusinessCategory businessCategory
) {}
