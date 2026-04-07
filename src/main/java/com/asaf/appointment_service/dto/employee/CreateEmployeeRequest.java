package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
    @NotNull(message = "İşletme ID zorunludur")
    Long businessId,

    @NotBlank(message = "Çalışan adı zorunludur")
    @Size(min = 2, max = 100, message = "Çalışan adı 2-100 karakter arasında olmalıdır")
    String name,

    String phoneNumber,

    String phoneCountryCode,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    String email,

    @Size(max = 500, message = "Biyografi en fazla 500 karakter olabilir")
    String bio,

    boolean owner
) {
    public CreateEmployeeRequest {
        if (phoneCountryCode == null || phoneCountryCode.isBlank()) {
            phoneCountryCode = "+90";
        }
    }
}
