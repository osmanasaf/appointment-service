package com.asaf.appointment_service.dto.service;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePackageTemplateRequest(
    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @NotBlank(message = "Paket adı zorunludur")
    @Size(min = 2, max = 100, message = "Paket adı 2-100 karakter arasında olmalıdır")
    String name,

    @NotNull(message = "Seans sayısı zorunludur")
    @Min(value = 1, message = "En az 1 seans olmalıdır")
    Integer totalSessions,

    @PositiveOrZero(message = "Fiyat negatif olamaz")
    BigDecimal price
) {}
