package com.asaf.appointment_service.dto.customer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record UpdatePackageRequest(
    @NotBlank(message = "Paket adı zorunludur")
    String name,

    @Future(message = "Bitiş tarihi gelecekte olmalıdır")
    LocalDateTime expiresAt,

    @NotNull(message = "Aktiflik durumu zorunludur")
    Boolean active
) {}
