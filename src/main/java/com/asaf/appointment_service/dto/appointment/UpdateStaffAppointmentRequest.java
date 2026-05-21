package com.asaf.appointment_service.dto.appointment;

import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateStaffAppointmentRequest(
    @NotNull(message = "Çalışan ID zorunludur")
    Long employeeId,

    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @NotNull(message = "Randevu zamanı zorunludur")
    @Future(message = "Randevu tarihi gelecekte olmalıdır")
    LocalDateTime scheduledAt,

    @Size(max = 500, message = "Notlar en fazla 500 karakter olabilir")
    String notes,

    AppointmentSource source
) {
}
