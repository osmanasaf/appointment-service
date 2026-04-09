package com.asaf.appointment_service.service.dashboard;

import com.asaf.appointment_service.dto.dashboard.DashboardPeriod;
import com.asaf.appointment_service.dto.dashboard.DashboardStatsResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final BusinessService businessService;

    public DashboardService(AppointmentRepository appointmentRepository, BusinessService businessService) {
        this.appointmentRepository = appointmentRepository;
        this.businessService = businessService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long businessId, DashboardPeriod period) {
        businessService.findByIdOrThrow(businessId);

        LocalDate today = LocalDate.now();
        LocalDate start = switch (period) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY);
            case MONTHLY -> today.withDayOfMonth(1);
        };
        LocalDate end = switch (period) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.SUNDAY);
            case MONTHLY -> today.withDayOfMonth(today.lengthOfMonth());
        };

        List<Appointment> appointments = appointmentRepository.findByBusinessIdAndDateRange(
            businessId,
            start.atStartOfDay(),
            end.plusDays(1).atStartOfDay()
        );

        return buildStats(appointments, start, end);
    }

    private DashboardStatsResponse buildStats(List<Appointment> appointments, LocalDate start, LocalDate end) {
        int[] statusCounts = countByStatus(appointments);
        int total = appointments.size();
        double noShowRate = total > 0 ? Math.round((double) statusCounts[4] / total * 100.0) : 0.0;

        return new DashboardStatsResponse(
            total,
            statusCounts[0], statusCounts[1], statusCounts[2], statusCounts[3], statusCounts[4],
            noShowRate,
            buildTrendData(appointments, start, end),
            buildServiceDistribution(appointments),
            buildHourDistribution(appointments)
        );
    }

    private int[] countByStatus(List<Appointment> appointments) {
        int pending = 0, confirmed = 0, completed = 0, cancelled = 0, noShow = 0;
        for (Appointment a : appointments) {
            switch (a.getStatus()) {
                case PENDING -> pending++;
                case CONFIRMED -> confirmed++;
                case COMPLETED -> completed++;
                case CANCELLED -> cancelled++;
                case NO_SHOW -> noShow++;
                default -> { }
            }
        }
        return new int[]{pending, confirmed, completed, cancelled, noShow};
    }

    private List<DashboardStatsResponse.TrendPoint> buildTrendData(List<Appointment> appointments,
                                                                     LocalDate start, LocalDate end) {
        Map<String, Integer> trendMap = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            trendMap.put(d.toString(), 0);
        }
        for (Appointment a : appointments) {
            trendMap.merge(a.getScheduledAt().toLocalDate().toString(), 1, Integer::sum);
        }
        return trendMap.entrySet().stream()
            .map(e -> new DashboardStatsResponse.TrendPoint(e.getKey(), e.getValue()))
            .toList();
    }

    private List<DashboardStatsResponse.ServiceCount> buildServiceDistribution(List<Appointment> appointments) {
        Map<Long, Integer> serviceMap = new HashMap<>();
        for (Appointment a : appointments) {
            serviceMap.merge(a.getServiceId(), 1, Integer::sum);
        }
        return serviceMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .map(e -> new DashboardStatsResponse.ServiceCount(e.getKey(), e.getValue()))
            .toList();
    }

    private List<Integer> buildHourDistribution(List<Appointment> appointments) {
        int[] hours = new int[24];
        for (Appointment a : appointments) {
            hours[a.getScheduledAt().getHour()]++;
        }
        return new ArrayList<>(Arrays.stream(hours).boxed().toList());
    }
}
