package com.asaf.appointment_service.integration.google.dto;

import java.time.LocalDateTime;

public record GoogleCalendarEventData(
    String summary,
    String description,
    LocalDateTime start,
    LocalDateTime end,
    String timeZone
) {}
