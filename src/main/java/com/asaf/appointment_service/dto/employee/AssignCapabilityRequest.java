package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.SkillLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AssignCapabilityRequest(
    @NotNull(message = "Hizmet ID zorunludur")
    Long serviceId,

    @Min(value = 5, message = "Özel süre en az 5 dakika olmalıdır")
    Integer customDurationMinutes,

    @Positive(message = "Özel fiyat pozitif olmalıdır")
    BigDecimal customPrice,

    SkillLevel skillLevel
) {}
