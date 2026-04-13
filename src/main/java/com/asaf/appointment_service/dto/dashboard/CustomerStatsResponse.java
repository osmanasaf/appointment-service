package com.asaf.appointment_service.dto.dashboard;

import java.util.List;

public record CustomerStatsResponse(
    int totalCustomers,
    int newCustomersInPeriod,
    int returningCustomers,
    double returnRate,
    int blacklistedCount,
    int riskyCount,
    List<TopCustomer> topCustomers
) {
    public record TopCustomer(
        Long customerId,
        String name,
        int appointmentCount,
        int noShowCount
    ) {}
}
