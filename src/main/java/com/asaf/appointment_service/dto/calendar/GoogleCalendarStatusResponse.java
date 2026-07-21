package com.asaf.appointment_service.dto.calendar;

import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import java.time.LocalDateTime;

public record GoogleCalendarStatusResponse(
    boolean connected,
    CalendarConnectionStatus status,
    LocalDateTime lastSyncedAt,
    String brokenReason
) {
    public static GoogleCalendarStatusResponse notConnected() {
        return new GoogleCalendarStatusResponse(false, null, null, null);
    }
}
