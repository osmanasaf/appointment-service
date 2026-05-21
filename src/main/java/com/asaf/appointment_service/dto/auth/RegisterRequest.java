package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli bir email giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @NotBlank(message = "Şifre zorunludur")
    @Pattern(regexp = ValidationPatterns.PASSWORD_REGEX, message = ValidationPatterns.PASSWORD_MESSAGE)
    String password,

    @NotBlank(message = "İsim zorunludur")
    @Size(min = 2, max = 100, message = "İsim 2-100 karakter olmalıdır")
    String name,

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    @NotBlank(message = "İşletme adı zorunludur")
    @Size(min = 2, max = 100, message = "İşletme adı 2-100 karakter olmalıdır")
    String businessName,

    @NotNull(message = "İşletme kategorisi zorunludur")
    BusinessCategory businessCategory
) {}
