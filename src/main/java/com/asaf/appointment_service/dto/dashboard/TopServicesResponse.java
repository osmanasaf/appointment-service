package com.asaf.appointment_service.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record TopServicesResponse(List<ServiceStat> services) {

    public record ServiceStat(
        Long serviceId,
        String serviceName,
        int totalAppointments,
        int completedCount,
        BigDecimal totalRevenue,
        double completionRate
    ) {}
}
