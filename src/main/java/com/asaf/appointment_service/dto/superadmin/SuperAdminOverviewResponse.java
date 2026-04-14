package com.asaf.appointment_service.dto.superadmin;

import java.util.List;

public record SuperAdminOverviewResponse(
    long totalBusinesses,
    long activeBusinesses,
    long totalUsers,
    long totalEmployees,
    SubscriptionBreakdown subscriptionBreakdown,
    List<RegistrationTrendPoint> registrationTrend
) {

    public record SubscriptionBreakdown(
        long trialCount,
        long activeCount,
        long expiredCount,
        long suspendedCount
    ) {}

    public record RegistrationTrendPoint(String date, long count) {}
}
