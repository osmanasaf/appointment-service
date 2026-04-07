package com.asaf.appointment_service.dto.customer;

import jakarta.validation.constraints.NotNull;

public record AssignSessionRequest(
    @NotNull(message = "Randevu ID zorunludur")
    Long appointmentId
) {}
