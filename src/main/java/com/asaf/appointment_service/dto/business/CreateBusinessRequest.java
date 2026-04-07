package com.asaf.appointment_service.dto.business;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBusinessRequest(
    @NotBlank(message = "İşletme adı zorunludur")
    @Size(min = 2, max = 100, message = "İşletme adı 2-100 karakter arasında olmalıdır")
    String name,

    @NotBlank(message = "Telefon numarası zorunludur")
    String phoneNumber,

    String phoneCountryCode,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    String email,

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    String address,

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    String description
) {
    public CreateBusinessRequest {
        if (phoneCountryCode == null || phoneCountryCode.isBlank()) {
            phoneCountryCode = "+90";
        }
    }
}
