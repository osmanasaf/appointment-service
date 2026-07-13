package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicAppointmentOtpRequest(
    @NotBlank(message = "Müşteri adı zorunludur")
    @Size(min = 2, max = 100, message = "İsim 2-100 karakter olmalıdır")
    String customerName,

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    String phoneCountryCode
) {
    public String getPhoneCountryCode() {
        return phoneCountryCode != null ? phoneCountryCode : "+90";
    }
}
