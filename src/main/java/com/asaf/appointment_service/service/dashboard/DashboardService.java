package com.asaf.appointment_service.service.dashboard;

import com.asaf.appointment_service.dto.dashboard.CustomerStatsResponse;
import com.asaf.appointment_service.dto.dashboard.DashboardPeriod;
import com.asaf.appointment_service.dto.dashboard.DashboardStatsResponse;
import com.asaf.appointment_service.dto.dashboard.PackageUsageResponse;
import com.asaf.appointment_service.dto.dashboard.TopServicesResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.CustomerRepository;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int EXPIRING_DAYS_THRESHOLD = 7;

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerPackageRepository packageRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessService businessService;

    public DashboardService(AppointmentRepository appointmentRepository,
                             CustomerRepository customerRepository,
                             CustomerPackageRepository packageRepository,
                             ServiceRepository serviceRepository,
                             BusinessService businessService) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.packageRepository = packageRepository;
        this.serviceRepository = serviceRepository;
        this.businessService = businessService;
    }

    // ── Ana istatistikler ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long businessId, DashboardPeriod period) {
        businessService.findByIdOrThrow(businessId);
        var range = buildDateRange(period);
        List<Appointment> appointments = appointmentRepository.findByBusinessIdAndDateRange(
            businessId, range[0], range[1]);
        return buildStats(appointments, range[0].toLocalDate(), range[1].minusDays(1).toLocalDate());
    }

    // ── En çok randevu alan hizmetler ────────────────────────────────────────

    @Transactional(readOnly = true)
    public TopServicesResponse getTopServices(Long businessId, DashboardPeriod period, int limit) {
        businessService.findByIdOrThrow(businessId);
        if (limit < 1 || limit > 20) {
            limit = 5;
        }
        var range = buildDateRange(period);

        List<Object[]> rawCounts = appointmentRepository.countByServiceIdForBusiness(
            businessId, range[0], range[1], PageRequest.of(0, limit));

        List<Object[]> completedCounts = appointmentRepository.countCompletedByServiceIdForBusiness(
            businessId, range[0], range[1]);

        Map<Long, Long> completedMap = new HashMap<>();
        for (Object[] row : completedCounts) {
            completedMap.put((Long) row[0], (Long) row[1]);
        }

        List<TopServicesResponse.ServiceStat> stats = rawCounts.stream()
            .map(row -> {
                Long serviceId = (Long) row[0];
                int total = ((Long) row[1]).intValue();
                int completed = completedMap.getOrDefault(serviceId, 0L).intValue();
                double completionRate = total > 0 ? Math.round((double) completed / total * 100.0) : 0.0;

                String serviceName = serviceRepository.findById(serviceId)
                    .map(ServiceDefinition::getName)
                    .orElse("Silinmiş hizmet #" + serviceId);

                BigDecimal totalRevenue = serviceRepository.findById(serviceId)
                    .map(s -> s.getPrice().multiply(BigDecimal.valueOf(completed)))
                    .orElse(BigDecimal.ZERO);

                return new TopServicesResponse.ServiceStat(
                    serviceId, serviceName, total, completed, totalRevenue, completionRate);
            })
            .toList();

        return new TopServicesResponse(stats);
    }

    // ── Müşteri istatistikleri ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerStatsResponse getCustomerStats(Long businessId, DashboardPeriod period) {
        businessService.findByIdOrThrow(businessId);
        var range = buildDateRange(period);

        List<Customer> allCustomers = customerRepository.findByBusinessIdOrderByNameAsc(businessId);
        int totalCustomers = allCustomers.size();

        // Dönem içinde yeni kayıt olan müşteriler
        LocalDateTime periodStart = range[0];
        int newCustomers = (int) allCustomers.stream()
            .filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(periodStart))
            .count();

        // Dönemde 2+ randevu olan müşteri sayısı (tekrar eden)
        long returningCustomers = appointmentRepository.countReturningCustomers(
            businessId, range[0], range[1]);

        // Dönemde randevusu olan unique müşteri sayısı
        long periodCustomers = appointmentRepository.countUniqueCustomers(
            businessId, range[0], range[1]);

        double returnRate = periodCustomers > 0
            ? Math.round((double) returningCustomers / periodCustomers * 100.0)
            : 0.0;

        int blacklistedCount = (int) allCustomers.stream().filter(Customer::isBlacklisted).count();
        int riskyCount = (int) allCustomers.stream().filter(Customer::isRisky).count();

        // En çok randevusu olan 5 müşteri
        List<Object[]> topRaw = appointmentRepository.findTopCustomersByAppointmentCount(
            businessId, range[0], range[1], PageRequest.of(0, 5));

        Map<Long, Customer> customerMap = new HashMap<>();
        allCustomers.forEach(c -> customerMap.put(c.getId(), c));

        List<CustomerStatsResponse.TopCustomer> topCustomers = topRaw.stream()
            .map(row -> {
                Long customerId = (Long) row[0];
                int count = ((Long) row[1]).intValue();
                Customer customer = customerMap.get(customerId);
                String name = customer != null ? customer.getName() : "Müşteri #" + customerId;
                int noShowCount = customer != null ? customer.getNoShowCount() : 0;
                return new CustomerStatsResponse.TopCustomer(customerId, name, count, noShowCount);
            })
            .toList();

        return new CustomerStatsResponse(
            totalCustomers, newCustomers, (int) returningCustomers,
            returnRate, blacklistedCount, riskyCount, topCustomers);
    }

    // ── Paket kullanım durumu ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PackageUsageResponse getPackageUsage(Long businessId) {
        businessService.findByIdOrThrow(businessId);

        List<CustomerPackage> activePackages = packageRepository.findByBusinessIdAndActiveTrue(businessId);

        LocalDateTime expiringThreshold = LocalDateTime.now().plusDays(EXPIRING_DAYS_THRESHOLD);

        int lowSessionCount = 0;
        int expiringCount = 0;
        double totalUsageRate = 0.0;

        // Customer ve Service isimlerini cache'le
        Map<Long, String> customerNameCache = new HashMap<>();
        Map<Long, String> serviceNameCache = new HashMap<>();

        List<PackageUsageResponse.PackageSummary> summaries = new ArrayList<>();

        for (CustomerPackage pkg : activePackages) {
            boolean isLow = pkg.isLowOnSessions();
            boolean isExpiring = pkg.getExpiresAt() != null && pkg.getExpiresAt().isBefore(expiringThreshold);

            if (isLow) lowSessionCount++;
            if (isExpiring) expiringCount++;

            int used = pkg.getTotalSessions() - pkg.getRemainingSessions();
            double usageRate = pkg.getTotalSessions() > 0
                ? (double) used / pkg.getTotalSessions()
                : 0.0;
            totalUsageRate += usageRate;

            String customerName = customerNameCache.computeIfAbsent(pkg.getCustomerId(),
                id -> customerRepository.findById(id)
                    .map(Customer::getName)
                    .orElse("Müşteri #" + id));

            String serviceName = serviceNameCache.computeIfAbsent(pkg.getServiceId(),
                id -> serviceRepository.findById(id)
                    .map(ServiceDefinition::getName)
                    .orElse("Hizmet #" + id));

            summaries.add(new PackageUsageResponse.PackageSummary(
                pkg.getId(),
                customerName,
                serviceName,
                pkg.getTotalSessions(),
                pkg.getRemainingSessions(),
                pkg.getExpiresAt(),
                isLow,
                isExpiring
            ));
        }

        double avgUsageRate = activePackages.isEmpty()
            ? 0.0
            : Math.round(totalUsageRate / activePackages.size() * 100.0);

        return new PackageUsageResponse(
            activePackages.size(), lowSessionCount, expiringCount, avgUsageRate, summaries);
    }

    // ── Yardımcı metodlar ────────────────────────────────────────────────────

    /** Döneme göre [start, end] tarih aralığı döndürür (end bir sonraki günün başı). */
    private LocalDateTime[] buildDateRange(DashboardPeriod period) {
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
        return new LocalDateTime[]{start.atStartOfDay(), end.plusDays(1).atStartOfDay()};
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
