package com.asaf.appointment_service.dto.business;

public record BusinessPhotoResponse(
    Long id,
    String url,
    String caption,
    int sortOrder
) {}
