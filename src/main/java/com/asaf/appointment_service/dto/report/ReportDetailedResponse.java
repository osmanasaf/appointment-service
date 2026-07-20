package com.asaf.appointment_service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportDetailedResponse(
    LocalDate periodStart,
    LocalDate periodEnd,
    int totalAppointments,
    int pending,
    int confirmed,
    int completed,
    int cancelled,
    int noShow,
    double noShowRatePercent,
    Double previousPeriodNoShowRatePercent,
    double occupancyRatePercent,
    BigDecimal estimatedRevenue,
    List<ServiceRevenueItem> revenueByService,
    List<EmployeeBreakdownItem> employeeBreakdown,
    Integer waitlistConvertedCount,
    Double averageReviewRating,
    int activePackages,
    int lowSessionPackages
) {
    public record ServiceRevenueItem(Long serviceId, String serviceName, int completedCount, BigDecimal revenue) {
    }

    public record EmployeeBreakdownItem(Long employeeId, String employeeName, int appointmentCount,
                                        double noShowRatePercent) {
    }
}
