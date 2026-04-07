package com.asaf.appointment_service.dto.customer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreatePackageRequest(
    @NotNull(message = "Müşteri ID zorunludur")
    Long customerId,

    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @NotBlank(message = "Paket adı zorunludur")
    String name,

    @NotNull(message = "Toplam seans sayısı zorunludur")
    @Min(value = 1, message = "En az 1 seans olmalıdır")
    Integer totalSessions,

    @Future(message = "Bitiş tarihi gelecekte olmalıdır")
    LocalDateTime expiresAt
) {}
