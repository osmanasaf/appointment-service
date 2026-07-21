package com.asaf.appointment_service.integration.google.dto;

import java.time.LocalDateTime;

public record GoogleBusyBlock(
    String externalEventId,
    LocalDateTime start,
    LocalDateTime end
) {}
