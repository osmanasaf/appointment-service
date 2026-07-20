package com.asaf.appointment_service.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PublicRescheduleRequest(
    @NotNull(message = "Çalışan ID zorunludur")
    Long employeeId,

    @NotNull(message = "Yeni randevu zamanı zorunludur")
    @Future(message = "Randevu tarihi gelecekte olmalıdır")
    LocalDateTime scheduledAt
) {
}
