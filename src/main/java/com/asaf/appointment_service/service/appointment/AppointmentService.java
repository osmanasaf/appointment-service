package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.service.BaseService;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.exception.appointment.CustomerDayLimitExceededException;
import com.asaf.appointment_service.exception.appointment.SlotNotAvailableException;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.domain.event.AppointmentCancelled;
import com.asaf.appointment_service.domain.event.AppointmentCompleted;
import com.asaf.appointment_service.domain.event.AppointmentConfirmed;
import com.asaf.appointment_service.domain.event.AppointmentCreated;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asaf.appointment_service.exception.employee.EmployeeOnLeaveException;
import com.asaf.appointment_service.repository.employee.EmployeeLeaveRepository;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Service
public class AppointmentService extends BaseService<Appointment, Long, AppointmentRepository> {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final BusinessService businessService;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final EmployeeCapabilityService employeeCapabilityService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final PackageService packageService;
    private final PackageSessionService packageSessionService;
    private final CustomerPackageRepository customerPackageRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final EmployeeScheduleService employeeScheduleService;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentRepository repository,
                              BusinessService businessService,
                              CustomerService customerService,
                              EmployeeService employeeService,
                              EmployeeCapabilityService employeeCapabilityService,
                              ServiceDefinitionService serviceDefinitionService,
                              PackageService packageService,
                              PackageSessionService packageSessionService,
                              CustomerPackageRepository customerPackageRepository,
                              EmployeeLeaveRepository employeeLeaveRepository,
                              EmployeeScheduleService employeeScheduleService,
                              ApplicationEventPublisher eventPublisher) {
        super(repository, "Randevu");
        this.businessService = businessService;
        this.customerService = customerService;
        this.employeeService = employeeService;
        this.employeeCapabilityService = employeeCapabilityService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.packageService = packageService;
        this.packageSessionService = packageSessionService;
        this.customerPackageRepository = customerPackageRepository;
        this.employeeLeaveRepository = employeeLeaveRepository;
        this.employeeScheduleService = employeeScheduleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment createAppointment(CreateAppointmentRequest request) {
        Business business = businessService.findByIdOrThrow(request.businessId());
        customerService.findByIdOrThrow(request.customerId());
        employeeService.findByIdOrThrow(request.employeeId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(request.serviceId());

        employeeCapabilityService.validateCapability(request.employeeId(), request.serviceId());

        int totalDuration = service.totalDurationMinutes();
        LocalDateTime endTime = request.scheduledAt().plusMinutes(totalDuration);

        validateSlotAvailability(request.employeeId(), request.scheduledAt(), endTime);

        Integer dayLimit = business.getMaxAppointmentsPerCustomerPerDay();
        if (dayLimit != null) {
            LocalDate date = request.scheduledAt().toLocalDate();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            long count = repository.countActiveByCustomerOnDate(request.customerId(), dayStart, dayEnd);
            if (count >= dayLimit) {
                throw new CustomerDayLimitExceededException(dayLimit, (int) count);
            }
        }

        Appointment appointment = AppointmentMapper.toEntity(request, totalDuration);

        var customer = customerService.findByIdOrThrow(request.customerId());
        boolean forcePending = customer.isNoShowForcesManualApproval();
        if (!forcePending && business.isAutoConfirm() && !service.isRequiresConfirmation()) {
            appointment.confirm();
            log.info("Randevu otomatik onaylandı: businessId={}", request.businessId());
        }

        Appointment saved = repository.save(appointment);
        eventPublisher.publishEvent(new AppointmentCreated(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(), saved.getEmployeeId(),
            saved.getServiceId(), saved.getScheduledAt(), saved.getEndAt(), saved.getStatus().name()));
        log.info("Randevu oluşturuldu: appointmentId={}, customerId={}, source={}",
            saved.getId(), request.customerId(), request.getSource());
        return saved;
    }

    @Transactional
    public Appointment createStaffAppointment(StaffAppointmentRequest request) {
        if (request.source() != AppointmentSource.PHONE && request.source() != AppointmentSource.WALK_IN) {
            throw new IllegalArgumentException("Staff randevusu sadece PHONE veya WALK_IN kaynağı ile oluşturulabilir");
        }
        var customer = customerService.findOrCreateByPhone(
            request.businessId(),
            request.customerName(),
            request.getPhoneCountryCode(),
            request.phoneNumber()
        );
        CreateAppointmentRequest internal = new CreateAppointmentRequest(
            request.businessId(),
            customer.getId(),
            request.employeeId(),
            request.serviceId(),
            request.scheduledAt(),
            request.notes(),
            null,
            request.source()
        );
        return createAppointment(internal);
    }

    @Transactional
    public Appointment confirmAppointment(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.confirm();
        Appointment saved = repository.save(appointment);
        eventPublisher.publishEvent(new AppointmentConfirmed(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(),
            saved.getScheduledAt(), saved.getConfirmedAt()));
        log.info("Randevu onaylandı: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.cancel(reason);
        Appointment saved = repository.save(appointment);
        eventPublisher.publishEvent(new AppointmentCancelled(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(),
            saved.getScheduledAt(), saved.getCancellationReason()));
        log.info("Randevu iptal edildi: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment reassignAppointmentEmployee(Long appointmentId, Long newEmployeeId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.reassignEmployee(newEmployeeId);
        Appointment saved = repository.save(appointment);
        log.info("Randevu yeniden atandı: appointmentId={}, newEmployeeId={}", appointmentId, newEmployeeId);
        return saved;
    }

    @Transactional
    public Appointment completeAppointment(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.complete();
        Appointment saved = repository.save(appointment);
        if (saved.getPackageSessionId() != null) {
            packageSessionService.markCompleted(saved.getPackageSessionId());
        }
        deductPackageSessionIfEligible(appointment);
        eventPublisher.publishEvent(new AppointmentCompleted(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(), saved.getServiceId(),
            saved.getScheduledAt(), saved.getCompletedAt()));
        log.info("Randevu tamamlandı: appointmentId={}", appointmentId);
        return saved;
    }

    private void deductPackageSessionIfEligible(Appointment appointment) {
        try {
            packageService.deductSessionForAppointment(appointment.getCustomerId(), appointment.getServiceId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Randevu tamamlandı ancak paket seans düşülemedi: customerId={}, serviceId={}, error={}",
                appointment.getCustomerId(), appointment.getServiceId(), e.getMessage());
        }
    }

    @Transactional
    public Appointment markAsNoShow(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.markAsNoShow();
        if (appointment.getPackageSessionId() != null) {
            packageSessionService.markNoShow(appointment.getPackageSessionId());
        }
        customerService.recordNoShow(appointment.getCustomerId());
        Appointment saved = repository.save(appointment);
        log.info("Randevu no-show işaretlendi: appointmentId={}, customerId={}", 
            appointmentId, appointment.getCustomerId());
        return saved;
    }

    @Transactional
    public Appointment markAsRisky(Long appointmentId, String reason) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.markAsRisky(reason);
        Appointment saved = repository.save(appointment);
        log.warn("Randevu riskli işaretlendi: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByBusinessId(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdOrderByScheduledAtDesc(businessId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findRiskyAppointments(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndRiskyTrue(businessId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDateRange(Long businessId, LocalDate startDate, LocalDate endDate) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndDateRange(
            businessId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByEmployeeAndDateRange(Long employeeId, LocalDate date) {
        return repository.findByEmployeeIdAndScheduledAtBetween(
            employeeId,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay()
        );
    }

    @Override
    public Appointment findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAppointmentAtSlot(Long customerId, LocalDateTime start, LocalDateTime end) {
        return !repository.findActiveByCustomerAndTimeOverlap(customerId, start, end).isEmpty();
    }

    @Transactional
    public Appointment assignPackageSession(Long appointmentId, Long sessionId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        packageService.assignSessionToAppointment(sessionId, appointmentId, appointment.getScheduledAt());
        appointment.setPackageSessionId(sessionId);
        return repository.save(appointment);
    }

    @Transactional
    public Appointment unassignPackageSession(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        if (appointment.getPackageSessionId() == null) {
            throw new IllegalArgumentException("Randevuya bağlı paket seansı yok");
        }
        Long sessionId = appointment.getPackageSessionId();
        packageService.unassignSessionFromAppointment(sessionId);
        appointment.setPackageSessionId(null);
        return repository.save(appointment);
    }

    private void validateSlotAvailability(Long employeeId, LocalDateTime start, LocalDateTime end) {
        List<Appointment> conflicts = repository.findConflictingAppointments(employeeId, start, end);
        if (!conflicts.isEmpty()) {
            throw new SlotNotAvailableException("Seçilen saatte başka bir randevu mevcut");
        }
        validateEmployeeNotOnLeave(employeeId, start);
        validateEmployeeSchedule(employeeId, start);
    }

    private void validateEmployeeSchedule(Long employeeId, LocalDateTime scheduledAt) {
        DayOfWeek day = scheduledAt.getDayOfWeek();
        employeeScheduleService.getScheduleForDay(employeeId, day).ifPresent(schedule -> {
            if (schedule.isClosed()) {
                throw new SlotNotAvailableException("Çalışan bu gün çalışmamaktadır");
            }
            LocalTime time = scheduledAt.toLocalTime();
            if (time.isBefore(schedule.getStartTime()) || !time.isBefore(schedule.getEndTime())) {
                throw new SlotNotAvailableException("Seçilen saat çalışanın mesai saatleri dışında");
            }
        });
    }

    private void validateEmployeeNotOnLeave(Long employeeId, LocalDateTime scheduledAt) {
        boolean onLeave = employeeLeaveRepository
            .findActiveByEmployeeIdAndDate(employeeId, scheduledAt.toLocalDate())
            .stream()
            .anyMatch(leave -> leave.overlapsWithDateTime(scheduledAt));
        if (onLeave) {
            throw new EmployeeOnLeaveException();
        }
    }
}
