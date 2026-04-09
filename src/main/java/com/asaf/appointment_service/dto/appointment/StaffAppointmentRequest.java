package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Personel tarafından telefon veya yerinde (manuel) alınan randevu için tek istek.
 * Müşteri adı ve telefon ile findOrCreate yapılır, randevu kaynağı (PHONE / WALK_IN) ile kaydedilir.
 */
public record StaffAppointmentRequest(
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
    String notes,

    @NotNull(message = "Randevu kaynağı zorunludur (PHONE veya WALK_IN)")
    AppointmentSource source
) {
    public String getPhoneCountryCode() {
        return phoneCountryCode != null ? phoneCountryCode : "+90";
    }
}
