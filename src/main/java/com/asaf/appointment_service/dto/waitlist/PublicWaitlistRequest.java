package com.asaf.appointment_service.dto.waitlist;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PublicWaitlistRequest(
    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    Long employeeId,

    LocalDate preferredFrom,

    LocalDate preferredUntil,

    @NotBlank(message = "Müşteri adı zorunludur")
    @Size(min = 2, max = 100, message = "İsim 2-100 karakter olmalıdır")
    String customerName,

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX,
        message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber,

    String phoneCountryCode,

    @NotBlank(message = "Doğrulama kodu zorunludur")
    @Pattern(regexp = ValidationPatterns.OTP_REGEX, message = "Doğrulama kodu 6 haneli olmalıdır")
    String otpCode
) {
    public String getPhoneCountryCode() {
        return phoneCountryCode != null ? phoneCountryCode : "+90";
    }
}
