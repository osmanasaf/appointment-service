package com.asaf.appointment_service.dto.customer;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
    @NotBlank(message = "Müşteri adı zorunludur")
    @Size(min = 2, max = 100, message = "Ad 2-100 karakter arasında olmalıdır")
    String name,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes,

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    String address,

    @Pattern(regexp = "|" + ValidationPatterns.NATIONAL_ID_REGEX,
        message = "TC kimlik numarası boş veya 11 rakam olmalıdır")
    String nationalId
) {
    public UpdateCustomerRequest {
        if (email != null && email.isBlank()) {
            email = null;
        }
        if (nationalId != null && nationalId.isBlank()) {
            nationalId = null;
        }
    }
}
