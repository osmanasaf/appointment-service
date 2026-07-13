package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PublicAppointmentRequest(
    @NotNull(message = "Çalışan ID zorunludur")
    Long employeeId,

    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @NotNull(message = "Randevu zamanı zorunludur")
    @Future(message = "Randevu tarihi gelecekte olmalıdır")
    LocalDateTime scheduledAt,

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
    String otpCode,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes
) {
    public String getPhoneCountryCode() {
        return phoneCountryCode != null ? phoneCountryCode : "+90";
    }
}
