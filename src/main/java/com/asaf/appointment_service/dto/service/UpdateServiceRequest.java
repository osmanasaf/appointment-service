package com.asaf.appointment_service.dto.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateServiceRequest(
    @NotBlank(message = "Hizmet adı zorunludur")
    @Size(min = 2, max = 100, message = "Hizmet adı 2-100 karakter arasında olmalıdır")
    String name,

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    String description,

    @Min(value = 15, message = "Süre en az 15 dakika olmalıdır")
    @Max(value = 360, message = "Süre en fazla 360 dakika olabilir")
    int durationMinutes,

    @PositiveOrZero(message = "Buffer süresi negatif olamaz")
    int bufferBeforeMinutes,

    @PositiveOrZero(message = "Buffer süresi negatif olamaz")
    int bufferAfterMinutes,

    @PositiveOrZero(message = "Fiyat negatif olamaz")
    BigDecimal price,

    boolean requiresConfirmation,

    boolean sameDayBookingAllowed,

    boolean packageEligible
) {}
