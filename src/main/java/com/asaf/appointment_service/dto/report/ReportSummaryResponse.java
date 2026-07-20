package com.asaf.appointment_service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
    LocalDate periodStart,
    LocalDate periodEnd,
    int totalAppointments,
    int pending,
    int confirmed,
    int completed,
    int cancelled,
    int noShow,
    double noShowRatePercent,
    BigDecimal estimatedRevenue,
    int activePackages,
    int lowSessionPackages
) {
}
