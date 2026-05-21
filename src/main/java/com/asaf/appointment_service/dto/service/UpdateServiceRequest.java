package com.asaf.appointment_service.dto.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateServiceRequest(
    @NotBlank(message = "Hizmet adı zorunludur")
    @Size(min = 2, max = 100, message = "Hizmet adı 2-100 karakter arasında olmalıdır")
    String name,

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    String description,

    @NotNull(message = "Süre zorunludur")
    @Min(value = 15, message = "Süre en az 15 dakika olmalıdır")
    @Max(value = 360, message = "Süre en fazla 360 dakika olabilir")
    Integer durationMinutes,

    @PositiveOrZero(message = "Buffer süresi negatif olamaz")
    Integer bufferBeforeMinutes,

    @PositiveOrZero(message = "Buffer süresi negatif olamaz")
    Integer bufferAfterMinutes,

    @PositiveOrZero(message = "Fiyat negatif olamaz")
    BigDecimal price,

    Boolean requiresConfirmation,

    Boolean sameDayBookingAllowed,

    Boolean packageEligible
) {
    public UpdateServiceRequest {
        if (bufferBeforeMinutes == null) {
            bufferBeforeMinutes = 0;
        }
        if (bufferAfterMinutes == null) {
            bufferAfterMinutes = 0;
        }
        if (requiresConfirmation == null) {
            requiresConfirmation = false;
        }
        if (sameDayBookingAllowed == null) {
            sameDayBookingAllowed = true;
        }
        if (packageEligible == null) {
            packageEligible = true;
        }
    }
}
