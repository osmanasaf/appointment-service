package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBusinessRequest(
    @NotBlank(message = "İşletme adı zorunludur")
    @Size(min = 2, max = 100, message = "İşletme adı 2-100 karakter arasında olmalıdır")
    String name,

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    String phoneCountryCode,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    String address,

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    String description,

    @NotNull(message = "İşletme kategorisi zorunludur")
    BusinessCategory category
) {
    public CreateBusinessRequest {
        if (phoneCountryCode == null || phoneCountryCode.isBlank()) {
            phoneCountryCode = "+90";
        }
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
