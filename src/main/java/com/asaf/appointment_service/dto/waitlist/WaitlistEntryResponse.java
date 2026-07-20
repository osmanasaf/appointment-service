package com.asaf.appointment_service.dto.waitlist;

import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WaitlistEntryResponse(
    Long id,
    Long businessId,
    Long customerId,
    Long serviceId,
    Long employeeId,
    LocalDate preferredFrom,
    LocalDate preferredUntil,
    WaitlistStatus status,
    LocalDateTime notifiedAt,
    LocalDateTime offeredScheduledAt,
    LocalDateTime createdAt
) {
}
