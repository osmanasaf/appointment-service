package com.asaf.appointment_service.service.report;

import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.dto.report.ReportDetailedResponse;
import com.asaf.appointment_service.dto.report.ReportSummaryResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long BUSINESS_ID = 1L;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private BusinessService businessService;
    @Mock
    private BusinessWorkingHoursRepository workingHoursRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private CustomerPackageRepository packageRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;
    @Mock
    private ReviewRepository reviewRepository;

    private ReportService reportService;

    private void setUp() {
        reportService = new ReportService(appointmentRepository, businessService, workingHoursRepository,
            employeeService, serviceRepository, packageRepository, subscriptionService, waitlistEntryRepository,
            reviewRepository);
        lenient().when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(mock(Business.class));
        lenient().when(packageRepository.findByBusinessIdAndActiveTrue(BUSINESS_ID)).thenReturn(List.of());
    }

    private Appointment appointment(AppointmentStatus status, Long employeeId, Long serviceId,
                                     LocalDateTime scheduledAt, int durationMinutes) {
        Appointment appointment = Appointment.builder()
            .businessId(BUSINESS_ID).customerId(10L).employeeId(employeeId).serviceId(serviceId)
            .timeSlot(scheduledAt, durationMinutes)
            .status(status)
            .build();
        ReflectionTestUtils.setField(appointment, "id", 1L);
        return appointment;
    }

    private ServiceDefinition serviceDefinition(Long id, BigDecimal price) {
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(BUSINESS_ID).name("Saç Kesimi").durationMinutes(60).price(price).build();
        ReflectionTestUtils.setField(service, "id", id);
        return service;
    }

    @Test
    void should_throwPlanFeatureNotAvailable_when_getSummaryCalledWithoutBasicReports() {
        setUp();
        doThrow(new PlanFeatureNotAvailableException(PlanFeature.BASIC_REPORTS))
            .when(subscriptionService).requireFeature(BUSINESS_ID, PlanFeature.BASIC_REPORTS);

        assertThatThrownBy(() -> reportService.getSummary(BUSINESS_ID))
            .isInstanceOf(PlanFeatureNotAvailableException.class);
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void should_computeCountsAndRevenue_when_getSummaryCalled() {
        setUp();
        LocalDateTime now = LocalDateTime.now().minusDays(5);
        List<Appointment> appointments = List.of(
            appointment(AppointmentStatus.COMPLETED, 2L, 3L, now, 60),
            appointment(AppointmentStatus.COMPLETED, 2L, 3L, now.plusHours(2), 60),
            appointment(AppointmentStatus.NO_SHOW, 2L, 3L, now.plusHours(4), 60),
            appointment(AppointmentStatus.CONFIRMED, 2L, 3L, now.plusHours(6), 60)
        );
        when(appointmentRepository.findByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
            .thenReturn(appointments);
        when(serviceRepository.findAllById(java.util.Set.of(3L)))
            .thenReturn(List.of(serviceDefinition(3L, BigDecimal.valueOf(100))));

        ReportSummaryResponse response = reportService.getSummary(BUSINESS_ID);

        assertThat(response.totalAppointments()).isEqualTo(4);
        assertThat(response.completed()).isEqualTo(2);
        assertThat(response.noShow()).isEqualTo(1);
        assertThat(response.confirmed()).isEqualTo(1);
        assertThat(response.noShowRatePercent()).isEqualTo(25.0);
        assertThat(response.estimatedRevenue()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void should_throwValidationException_when_getDetailedCalledWithEndBeforeStart() {
        setUp();
        LocalDate start = LocalDate.now();
        LocalDate end = start.minusDays(1);

        assertThatThrownBy(() -> reportService.getDetailed(BUSINESS_ID, start, end, null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_throwValidationException_when_getDetailedRangeExceedsMax() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(400);
        LocalDate end = LocalDate.now();

        assertThatThrownBy(() -> reportService.getDetailed(BUSINESS_ID, start, end, null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_returnNullWaitlistAndReviewMetrics_when_featuresDisabled() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(appointmentRepository.findByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
            .thenReturn(List.of());
        when(employeeService.findActiveByBusinessId(BUSINESS_ID)).thenReturn(List.of());
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.WAITLIST)).thenReturn(false);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(false);

        ReportDetailedResponse response = reportService.getDetailed(BUSINESS_ID, start, end, null);

        assertThat(response.waitlistConvertedCount()).isNull();
        assertThat(response.averageReviewRating()).isNull();
    }

    @Test
    void should_includeWaitlistAndReviewMetrics_when_featuresEnabled() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(appointmentRepository.findByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
            .thenReturn(List.of());
        when(employeeService.findActiveByBusinessId(BUSINESS_ID)).thenReturn(List.of());
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.WAITLIST)).thenReturn(true);
        when(subscriptionService.isFeatureEnabled(BUSINESS_ID, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(true);
        when(reviewRepository.findAverageRating(BUSINESS_ID)).thenReturn(4.5);

        WaitlistEntry converted = WaitlistEntry.builder()
            .businessId(BUSINESS_ID).customerId(10L).serviceId(3L).build();
        ReflectionTestUtils.setField(converted, "status", WaitlistStatus.CONVERTED);
        ReflectionTestUtils.setField(converted, "convertedAt", start.plusDays(1).atStartOfDay());
        when(waitlistEntryRepository.findByBusinessIdAndStatusOrderByCreatedAtAsc(BUSINESS_ID, WaitlistStatus.CONVERTED))
            .thenReturn(List.of(converted));

        ReportDetailedResponse response = reportService.getDetailed(BUSINESS_ID, start, end, null);

        assertThat(response.waitlistConvertedCount()).isEqualTo(1);
        assertThat(response.averageReviewRating()).isEqualTo(4.5);
    }

    @Test
    void should_computeOccupancyRate_when_workingHoursAndAppointmentsExist() {
        setUp();
        LocalDate day = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate start = day;
        LocalDate end = day;
        LocalDateTime scheduledAt = day.atTime(10, 0);
        List<Appointment> appointments = List.of(
            appointment(AppointmentStatus.COMPLETED, 2L, 3L, scheduledAt, 60)
        );
        when(appointmentRepository.findByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
            .thenReturn(appointments);
        Employee employee = Employee.builder().id(2L).businessId(BUSINESS_ID).name("Ayşe").build();
        when(employeeService.findActiveByBusinessId(BUSINESS_ID)).thenReturn(List.of(employee));
        BusinessWorkingHours hours = new BusinessWorkingHours(
            BUSINESS_ID, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(workingHoursRepository.findByBusinessIdOrderByDayOfWeek(BUSINESS_ID)).thenReturn(List.of(hours));
        when(subscriptionService.isFeatureEnabled(eq(BUSINESS_ID), eq(PlanFeature.WAITLIST))).thenReturn(false);
        when(subscriptionService.isFeatureEnabled(eq(BUSINESS_ID), eq(PlanFeature.CUSTOMER_REVIEWS))).thenReturn(false);
        when(serviceRepository.findAllById(java.util.Set.of(3L)))
            .thenReturn(List.of(serviceDefinition(3L, BigDecimal.valueOf(100))));

        ReportDetailedResponse response = reportService.getDetailed(BUSINESS_ID, start, end, null);

        // 60 dakika dolu / (9 saat = 540 dakika * 1 çalışan) = %11.11
        assertThat(response.occupancyRatePercent()).isEqualTo(11.11);
    }

    @Test
    void should_filterByEmployee_when_employeeIdProvided() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().minusDays(1);
        List<Appointment> appointments = List.of(
            appointment(AppointmentStatus.COMPLETED, 2L, 3L, now, 60),
            appointment(AppointmentStatus.COMPLETED, 5L, 3L, now.plusHours(1), 60)
        );
        when(appointmentRepository.findByBusinessIdAndDateRange(eq(BUSINESS_ID), any(), any()))
            .thenReturn(appointments);
        when(employeeService.findActiveByBusinessId(BUSINESS_ID)).thenReturn(List.of());
        when(subscriptionService.isFeatureEnabled(eq(BUSINESS_ID), eq(PlanFeature.WAITLIST))).thenReturn(false);
        when(subscriptionService.isFeatureEnabled(eq(BUSINESS_ID), eq(PlanFeature.CUSTOMER_REVIEWS))).thenReturn(false);
        when(serviceRepository.findAllById(java.util.Set.of(3L)))
            .thenReturn(List.of(serviceDefinition(3L, BigDecimal.valueOf(100))));

        ReportDetailedResponse response = reportService.getDetailed(BUSINESS_ID, start, end, 2L);

        assertThat(response.totalAppointments()).isEqualTo(1);
        assertThat(response.employeeBreakdown()).isEmpty();
    }

    @Test
    void should_sumServicePrices_when_calculateWaitlistRecoveredRevenueCalled() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        WaitlistEntry converted = WaitlistEntry.builder()
            .businessId(BUSINESS_ID).customerId(10L).serviceId(3L).build();
        ReflectionTestUtils.setField(converted, "status", WaitlistStatus.CONVERTED);
        ReflectionTestUtils.setField(converted, "convertedAt", LocalDateTime.now().minusDays(5));
        when(waitlistEntryRepository.findByBusinessIdAndStatusOrderByCreatedAtAsc(BUSINESS_ID, WaitlistStatus.CONVERTED))
            .thenReturn(List.of(converted));
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(BUSINESS_ID).name("Saç Kesimi").durationMinutes(60).price(BigDecimal.valueOf(150)).build();
        when(serviceRepository.findById(3L)).thenReturn(java.util.Optional.of(service));

        BigDecimal recovered = reportService.calculateWaitlistRecoveredRevenue(BUSINESS_ID, start, end);

        assertThat(recovered).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void should_excludeOutOfRangeConversions_when_calculateWaitlistRecoveredRevenueCalled() {
        setUp();
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now();
        WaitlistEntry outOfRange = WaitlistEntry.builder()
            .businessId(BUSINESS_ID).customerId(10L).serviceId(3L).build();
        ReflectionTestUtils.setField(outOfRange, "status", WaitlistStatus.CONVERTED);
        ReflectionTestUtils.setField(outOfRange, "convertedAt", LocalDateTime.now().minusDays(60));
        when(waitlistEntryRepository.findByBusinessIdAndStatusOrderByCreatedAtAsc(BUSINESS_ID, WaitlistStatus.CONVERTED))
            .thenReturn(List.of(outOfRange));

        BigDecimal recovered = reportService.calculateWaitlistRecoveredRevenue(BUSINESS_ID, start, end);

        assertThat(recovered).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
