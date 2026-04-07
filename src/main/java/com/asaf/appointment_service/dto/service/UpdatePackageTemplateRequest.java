package com.asaf.appointment_service.dto.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdatePackageTemplateRequest(
    @Size(min = 2, max = 100, message = "Paket adı 2-100 karakter arasında olmalıdır")
    String name,

    @Min(value = 1, message = "Seans sayısı en az 1 olmalıdır")
    Integer totalSessions,

    @PositiveOrZero(message = "Fiyat negatif olamaz")
    BigDecimal price
) {}
