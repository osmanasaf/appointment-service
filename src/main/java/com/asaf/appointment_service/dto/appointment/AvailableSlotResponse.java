package com.asaf.appointment_service.dto.appointment;

import java.time.LocalDateTime;

public record AvailableSlotResponse(
    LocalDateTime startTime,
    LocalDateTime endTime,
    int durationMinutes,
    boolean instantConfirmation,
    String label
) {
    public static AvailableSlotResponse instant(LocalDateTime start, LocalDateTime end, int duration) {
        return new AvailableSlotResponse(start, end, duration, true, "🟢 Anında onay");
    }

    public static AvailableSlotResponse requiresConfirmation(LocalDateTime start, LocalDateTime end, int duration) {
        return new AvailableSlotResponse(start, end, duration, false, "⚠️ İşletme onayı");
    }
}
