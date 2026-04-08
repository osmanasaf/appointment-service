package com.asaf.appointment_service.dto.dashboard;

import java.util.List;

public record DashboardStatsResponse(
    int totalAppointments,
    int pendingCount,
    int confirmedCount,
    int completedCount,
    int cancelledCount,
    int noShowCount,
    double noShowRate,
    List<TrendPoint> trendData,
    List<ServiceCount> serviceDistribution,
    List<Integer> hourDistribution
) {
    public record TrendPoint(String date, int count) {}

    public record ServiceCount(Long serviceId, int count) {}
}
