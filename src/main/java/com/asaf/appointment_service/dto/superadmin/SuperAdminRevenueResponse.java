package com.asaf.appointment_service.dto.superadmin;

import java.math.BigDecimal;
import java.util.List;

public record SuperAdminRevenueResponse(
    BigDecimal totalMonthlyPotential,
    List<PlanRevenueBreakdown> planBreakdown
) {

    public record PlanRevenueBreakdown(
        String planCode,
        long businessCount,
        BigDecimal monthlyPrice,
        BigDecimal totalRevenue
    ) {}
}
