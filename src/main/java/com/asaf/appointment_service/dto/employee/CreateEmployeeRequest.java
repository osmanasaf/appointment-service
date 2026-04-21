package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
    @NotBlank(message = "Çalışan adı zorunludur")
    @Size(min = 2, max = 100, message = "Çalışan adı 2-100 karakter arasında olmalıdır")
    String name,

    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    String phoneCountryCode,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @Size(max = 500, message = "Biyografi en fazla 500 karakter olabilir")
    String bio,

    boolean owner
) {
    public CreateEmployeeRequest {
        if (phoneCountryCode == null || phoneCountryCode.isBlank()) {
            phoneCountryCode = "+90";
        }
        if (phoneNumber != null && phoneNumber.isBlank()) {
            phoneNumber = null;
        }
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
