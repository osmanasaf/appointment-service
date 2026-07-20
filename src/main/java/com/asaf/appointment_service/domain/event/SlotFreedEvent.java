package com.asaf.appointment_service.domain.event;

import java.time.LocalDateTime;

/**
 * Bir randevu iptal edildiğinde veya ertelenip eski saati boşaldığında yayınlanır.
 * Waitlist eşleştirmesini tetikler.
 */
public record SlotFreedEvent(
    Long businessId,
    Long employeeId,
    Long serviceId,
    LocalDateTime scheduledAt,
    LocalDateTime endAt
) {}
