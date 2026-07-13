package com.asaf.appointment_service.dto.payment;

import jakarta.validation.constraints.NotNull;

public record ToggleAutoRenewRequest(
    @NotNull Boolean autoRenew
) {}
