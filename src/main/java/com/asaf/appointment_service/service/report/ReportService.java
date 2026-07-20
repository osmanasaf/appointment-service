package com.asaf.appointment_service.service.report;

import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.dto.report.ReportDetailedResponse;
import com.asaf.appointment_service.dto.report.ReportDetailedResponse.EmployeeBreakdownItem;
import com.asaf.appointment_service.dto.report.ReportDetailedResponse.ServiceRevenueItem;
import com.asaf.appointment_service.dto.report.ReportSummaryResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.review.ReviewRepository;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * İşletme performans raporları. Temel (BASIC_REPORTS) özet tüm planlarda,
 * serbest tarih aralıklı detaylı rapor (ADVANCED_REPORTS) PRO+ planlarda.
 */
@Service
public class ReportService {

    private static final int BASIC_LOOKBACK_DAYS = 30;
    private static final int MAX_DETAILED_RANGE_DAYS = 366;

    private final AppointmentRepository appointmentRepository;
    private final BusinessService businessService;
    private final BusinessWorkingHoursRepository workingHoursRepository;
    private final EmployeeService employeeService;
    private final ServiceRepository serviceRepository;
    private final CustomerPackageRepository packageRepository;
    private final SubscriptionService subscriptionService;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ReviewRepository reviewRepository;

    public ReportService(AppointmentRepository appointmentRepository,
                         BusinessService businessService,
                         BusinessWorkingHoursRepository workingHoursRepository,
                         EmployeeService employeeService,
                         ServiceRepository serviceRepository,
                         CustomerPackageRepository packageRepository,
                         SubscriptionService subscriptionService,
                         WaitlistEntryRepository waitlistEntryRepository,
                         ReviewRepository reviewRepository) {
        this.appointmentRepository = appointmentRepository;
        this.businessService = businessService;
        this.workingHoursRepository = workingHoursRepository;
        this.employeeService = employeeService;
        this.serviceRepository = serviceRepository;
        this.packageRepository = packageRepository;
        this.subscriptionService = subscriptionService;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(Long businessId) {
        subscriptionService.requireFeature(businessId, PlanFeature.BASIC_REPORTS);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(BASIC_LOOKBACK_DAYS);

        List<Appointment> appointments = fetchRange(businessId, start, end);
        StatusCounts counts = StatusCounts.of(appointments);
        BigDecimal revenue = calculateRevenue(appointments);
        PackageSnapshot packages = calculatePackageSnapshot(businessId);

        return new ReportSummaryResponse(
            start, end, counts.total(), counts.pending, counts.confirmed, counts.completed,
            counts.cancelled, counts.noShow, counts.noShowRatePercent(), revenue,
            packages.activeCount, packages.lowSessionCount
        );
    }

    @Transactional(readOnly = true)
    public ReportDetailedResponse getDetailed(Long businessId, LocalDate startDate, LocalDate endDate, Long employeeId) {
        subscriptionService.requireFeature(businessId, PlanFeature.ADVANCED_REPORTS);
        validateRange(startDate, endDate);

        List<Appointment> appointments = fetchRange(businessId, startDate, endDate);
        if (employeeId != null) {
            appointments = appointments.stream()
                .filter(a -> employeeId.equals(a.getEmployeeId()))
                .toList();
        }

        StatusCounts counts = StatusCounts.of(appointments);
        Double previousNoShowRate = calculatePreviousPeriodNoShowRate(businessId, startDate, endDate, employeeId);
        List<Employee> activeEmployees = employeeService.findActiveByBusinessId(businessId);
        double occupancy = calculateOccupancyRate(businessId, startDate, endDate, appointments, activeEmployees);
        BigDecimal revenue = calculateRevenue(appointments);
        List<ServiceRevenueItem> revenueByService = calculateRevenueByService(appointments);
        List<EmployeeBreakdownItem> employeeBreakdown =
            employeeId == null ? calculateEmployeeBreakdown(activeEmployees, appointments) : List.of();
        PackageSnapshot packages = calculatePackageSnapshot(businessId);

        Integer waitlistConvertedCount = subscriptionService.isFeatureEnabled(businessId, PlanFeature.WAITLIST)
            ? countWaitlistConversions(businessId, startDate, endDate)
            : null;
        Double averageReviewRating = subscriptionService.isFeatureEnabled(businessId, PlanFeature.CUSTOMER_REVIEWS)
            ? reviewRepository.findAverageRating(businessId)
            : null;

        return new ReportDetailedResponse(
            startDate, endDate, counts.total(), counts.pending, counts.confirmed, counts.completed,
            counts.cancelled, counts.noShow, counts.noShowRatePercent(), previousNoShowRate,
            occupancy, revenue, revenueByService, employeeBreakdown,
            waitlistConvertedCount, averageReviewRating,
            packages.activeCount, packages.lowSessionCount
        );
    }

    private List<Appointment> fetchRange(Long businessId, LocalDate start, LocalDate end) {
        businessService.findByIdOrThrow(businessId);
        return appointmentRepository.findByBusinessIdAndDateRange(
            businessId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new ValidationException("REPORT_RANGE_REQUIRED", "Başlangıç ve bitiş tarihi zorunludur");
        }
        if (end.isBefore(start)) {
            throw new ValidationException("REPORT_RANGE_INVALID", "Bitiş tarihi başlangıçtan önce olamaz");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DETAILED_RANGE_DAYS) {
            throw new ValidationException("REPORT_RANGE_TOO_WIDE",
                "Rapor aralığı en fazla %d gün olabilir".formatted(MAX_DETAILED_RANGE_DAYS));
        }
    }

    private Double calculatePreviousPeriodNoShowRate(Long businessId, LocalDate start, LocalDate end, Long employeeId) {
        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(periodDays - 1);
        List<Appointment> previous = fetchRange(businessId, previousStart, previousEnd);
        if (employeeId != null) {
            previous = previous.stream().filter(a -> employeeId.equals(a.getEmployeeId())).toList();
        }
        if (previous.isEmpty()) {
            return null;
        }
        return StatusCounts.of(previous).noShowRatePercent();
    }

    /**
     * Doluluk oranı = (dolu/tamamlanan randevu süresi) / (işletmenin dönemdeki toplam
     * çalışma dakikası × aktif çalışan sayısı). Yaklaşık bir metriktir; çalışan bazlı
     * izin/program farklılıklarını hesaba katmaz.
     */
    private double calculateOccupancyRate(Long businessId, LocalDate start, LocalDate end,
                                           List<Appointment> appointments, List<Employee> activeEmployees) {
        long bookedMinutes = appointments.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CONFIRMED)
            .mapToLong(a -> Duration.between(a.getScheduledAt(), a.getEndAt()).toMinutes())
            .sum();

        if (activeEmployees.isEmpty()) {
            return 0.0;
        }
        Map<DayOfWeek, BusinessWorkingHours> hoursByDay = new HashMap<>();
        for (BusinessWorkingHours hours : workingHoursRepository.findByBusinessIdOrderByDayOfWeek(businessId)) {
            hoursByDay.put(hours.getDayOfWeek(), hours);
        }

        long availableMinutesPerEmployee = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            BusinessWorkingHours hours = hoursByDay.get(day.getDayOfWeek());
            if (hours == null || hours.isClosed()) {
                continue;
            }
            availableMinutesPerEmployee += workingMinutes(hours);
        }
        long totalAvailableMinutes = availableMinutesPerEmployee * activeEmployees.size();
        if (totalAvailableMinutes <= 0) {
            return 0.0;
        }
        return round2((double) bookedMinutes / totalAvailableMinutes * 100.0);
    }

    private long workingMinutes(BusinessWorkingHours hours) {
        long total = Duration.between(hours.getStartTime(), hours.getEndTime()).toMinutes();
        if (hours.getBreakStart() != null && hours.getBreakEnd() != null) {
            total -= Duration.between(hours.getBreakStart(), hours.getBreakEnd()).toMinutes();
        }
        return Math.max(0, total);
    }

    private BigDecimal calculateRevenue(List<Appointment> appointments) {
        return calculateRevenueByService(appointments).stream()
            .map(ServiceRevenueItem::revenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ServiceRevenueItem> calculateRevenueByService(List<Appointment> appointments) {
        Map<Long, Integer> completedCountByService = new HashMap<>();
        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                completedCountByService.merge(appointment.getServiceId(), 1, Integer::sum);
            }
        }
        Map<Long, ServiceDefinition> servicesById = new HashMap<>();
        for (ServiceDefinition service : serviceRepository.findAllById(completedCountByService.keySet())) {
            servicesById.put(service.getId(), service);
        }
        return completedCountByService.entrySet().stream()
            .map(entry -> {
                Long serviceId = entry.getKey();
                int count = entry.getValue();
                ServiceDefinition service = servicesById.get(serviceId);
                String name = service != null ? service.getName() : "Silinmiş hizmet #" + serviceId;
                BigDecimal price = service != null && service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO;
                return new ServiceRevenueItem(serviceId, name, count, price.multiply(BigDecimal.valueOf(count)));
            })
            .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
            .toList();
    }

    private List<EmployeeBreakdownItem> calculateEmployeeBreakdown(List<Employee> activeEmployees,
                                                                     List<Appointment> appointments) {
        Map<Long, List<Appointment>> byEmployee = new HashMap<>();
        for (Appointment appointment : appointments) {
            byEmployee.computeIfAbsent(appointment.getEmployeeId(), k -> new ArrayList<>()).add(appointment);
        }
        Map<Long, String> nameById = new HashMap<>();
        for (Employee employee : activeEmployees) {
            nameById.put(employee.getId(), employee.getName());
        }
        return byEmployee.entrySet().stream()
            .map(entry -> {
                Long employeeId = entry.getKey();
                StatusCounts counts = StatusCounts.of(entry.getValue());
                String name = nameById.getOrDefault(employeeId, "Çalışan #" + employeeId);
                return new EmployeeBreakdownItem(employeeId, name, counts.total(), counts.noShowRatePercent());
            })
            .sorted((a, b) -> Integer.compare(b.appointmentCount(), a.appointmentCount()))
            .toList();
    }

    private PackageSnapshot calculatePackageSnapshot(Long businessId) {
        List<CustomerPackage> activePackages = packageRepository.findByBusinessIdAndActiveTrue(businessId);
        int lowSessionCount = (int) activePackages.stream().filter(CustomerPackage::isLowOnSessions).count();
        return new PackageSnapshot(activePackages.size(), lowSessionCount);
    }

    private int countWaitlistConversions(Long businessId, LocalDate start, LocalDate end) {
        return waitlistConversionsInRange(businessId, start, end).size();
    }

    /**
     * Dönemde bekleme listesinden dönüştürülen randevuların tahmini hizmet bedeli toplamı.
     * Aylık özet mesajındaki "kurtarılan tutar" metriği için kullanılır.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateWaitlistRecoveredRevenue(Long businessId, LocalDate start, LocalDate end) {
        return waitlistConversionsInRange(businessId, start, end).stream()
            .map(entry -> serviceRepository.findById(entry.getServiceId())
                .map(ServiceDefinition::getPrice)
                .orElse(BigDecimal.ZERO))
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<WaitlistEntry> waitlistConversionsInRange(
            Long businessId, LocalDate start, LocalDate end) {
        return waitlistEntryRepository.findByBusinessIdAndStatusOrderByCreatedAtAsc(businessId, WaitlistStatus.CONVERTED)
            .stream()
            .filter(entry -> {
                LocalDate convertedDate = entry.getConvertedAt() != null ? entry.getConvertedAt().toLocalDate() : null;
                return convertedDate != null && !convertedDate.isBefore(start) && !convertedDate.isAfter(end);
            })
            .toList();
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record PackageSnapshot(int activeCount, int lowSessionCount) {
    }

    private record StatusCounts(int pending, int confirmed, int completed, int cancelled, int noShow) {
        static StatusCounts of(List<Appointment> appointments) {
            int pending = 0, confirmed = 0, completed = 0, cancelled = 0, noShow = 0;
            for (Appointment appointment : appointments) {
                switch (appointment.getStatus()) {
                    case PENDING, RISKY, RESCHEDULE_REQUESTED -> pending++;
                    case CONFIRMED -> confirmed++;
                    case COMPLETED -> completed++;
                    case CANCELLED -> cancelled++;
                    case NO_SHOW -> noShow++;
                }
            }
            return new StatusCounts(pending, confirmed, completed, cancelled, noShow);
        }

        int total() {
            return pending + confirmed + completed + cancelled + noShow;
        }

        double noShowRatePercent() {
            int total = total();
            return total > 0 ? round2((double) noShow / total * 100.0) : 0.0;
        }
    }
}
