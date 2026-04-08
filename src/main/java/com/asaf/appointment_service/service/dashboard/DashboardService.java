package com.asaf.appointment_service.service.dashboard;

import com.asaf.appointment_service.dto.dashboard.DashboardPeriod;
import com.asaf.appointment_service.dto.dashboard.DashboardStatsResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
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
        int pending = 0;
        int confirmed = 0;
        int completed = 0;
        int cancelled = 0;
        int noShow = 0;

        Map<String, Integer> trendMap = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            trendMap.put(d.toString(), 0);
        }

        Map<Long, Integer> serviceMap = new HashMap<>();
        int[] hours = new int[24];

        for (Appointment a : appointments) {
            AppointmentStatus status = a.getStatus();
            if (status == AppointmentStatus.PENDING) pending++;
            else if (status == AppointmentStatus.CONFIRMED) confirmed++;
            else if (status == AppointmentStatus.COMPLETED) completed++;
            else if (status == AppointmentStatus.CANCELLED) cancelled++;
            else if (status == AppointmentStatus.NO_SHOW) noShow++;

            String dayKey = a.getScheduledAt().toLocalDate().toString();
            trendMap.merge(dayKey, 1, Integer::sum);

            serviceMap.merge(a.getServiceId(), 1, Integer::sum);

            int hour = a.getScheduledAt().getHour();
            hours[hour]++;
        }

        int total = appointments.size();
        double noShowRate = total > 0 ? Math.round((double) noShow / total * 100.0) : 0.0;

        List<DashboardStatsResponse.TrendPoint> trendData = trendMap.entrySet().stream()
            .map(e -> new DashboardStatsResponse.TrendPoint(e.getKey(), e.getValue()))
            .toList();

        List<DashboardStatsResponse.ServiceCount> serviceDist = serviceMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .map(e -> new DashboardStatsResponse.ServiceCount(e.getKey(), e.getValue()))
            .toList();

        List<Integer> hourDist = new ArrayList<>(Arrays.stream(hours).boxed().toList());

        return new DashboardStatsResponse(
            total, pending, confirmed, completed, cancelled, noShow, noShowRate,
            trendData, serviceDist, hourDist
        );
    }
}
