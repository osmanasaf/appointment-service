package com.asaf.appointment_service.dto.customer;

import com.asaf.appointment_service.entity.customer.PackageSessionStatus;
import java.time.LocalDateTime;

public record PackageSessionResponse(
    Long id,
    Long customerPackageId,
    int sessionNumber,
    PackageSessionStatus status,
    Long appointmentId,
    LocalDateTime scheduledAt,
    String notes
) {}
