package com.asaf.appointment_service.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @Size(max = 100, message = "İsim en fazla 100 karakter olabilir")
    String customerName,

    @NotBlank(message = "Telefon numarası zorunludur")
    String phoneNumber,

    String phoneCountryCode,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes
) {
    public String getPhoneCountryCode() {
        return phoneCountryCode != null ? phoneCountryCode : "+90";
    }
}
