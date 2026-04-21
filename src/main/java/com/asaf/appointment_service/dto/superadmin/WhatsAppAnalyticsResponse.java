package com.asaf.appointment_service.dto.superadmin;

import java.util.List;

public record WhatsAppAnalyticsResponse(
    long totalSentThisMonth,
    List<BusinessWhatsAppUsage> perBusinessBreakdown,
    List<TypeBreakdown> typeBreakdown,
    List<DailyTrendPoint> dailyTrend
) {
    public record BusinessWhatsAppUsage(
        Long businessId,
        String businessName,
        long messageCount
    ) {}

    public record TypeBreakdown(
        String notificationType,
        long count
    ) {}

    public record DailyTrendPoint(
        String date,
        long count
    ) {}
}
