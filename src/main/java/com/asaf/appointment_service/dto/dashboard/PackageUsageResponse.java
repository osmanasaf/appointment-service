package com.asaf.appointment_service.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record PackageUsageResponse(
    int totalActivePackages,
    int lowSessionPackages,
    int expiringPackages,
    double avgUsageRate,
    List<PackageSummary> packages
) {
    public record PackageSummary(
        Long packageId,
        String customerName,
        String serviceName,
        int totalSessions,
        int remainingSessions,
        LocalDateTime expiresAt,
        boolean isLow,
        boolean isExpiring
    ) {}
}
