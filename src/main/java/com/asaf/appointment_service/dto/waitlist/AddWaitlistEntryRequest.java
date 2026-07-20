package com.asaf.appointment_service.dto.waitlist;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AddWaitlistEntryRequest(
    @NotNull(message = "Müşteri ID zorunludur")
    Long customerId,

    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    Long employeeId,

    LocalDate preferredFrom,

    LocalDate preferredUntil
) {
}
