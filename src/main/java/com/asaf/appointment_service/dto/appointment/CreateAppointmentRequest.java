package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateAppointmentRequest(
    @NotNull(message = "Müşteri ID zorunludur")
    Long customerId,

    @NotNull(message = "Çalışan ID zorunludur")
    Long employeeId,

    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @NotNull(message = "Randevu zamanı zorunludur")
    @Future(message = "Randevu tarihi gelecekte olmalıdır")
    LocalDateTime scheduledAt,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes,

    @Size(max = 500, message = "Müşteri notları en fazla 500 karakter olabilir")
    String customerNotes,

    /** Randevu kaynağı: ONLINE, PHONE, WALK_IN, OTHER. Verilmezse ONLINE kabul edilir. */
    AppointmentSource source
) {
    public AppointmentSource getSource() {
        return source != null ? source : AppointmentSource.ONLINE;
    }
}
