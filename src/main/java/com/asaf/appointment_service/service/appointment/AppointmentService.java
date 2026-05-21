package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.constants.AppointmentErrorMessages;
import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.common.service.BaseService;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.UpdateStaffAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.mapper.appointment.AppointmentMapper;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.domain.event.AppointmentCreated;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService extends BaseService<Appointment, Long, AppointmentRepository> {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final BusinessService businessService;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final PackageService packageService;
    private final PackageSessionService packageSessionService;
    private final AppointmentValidationService validationService;
    private final AppointmentStateService stateService;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentRepository repository,
                              BusinessService businessService,
                              CustomerService customerService,
                              EmployeeService employeeService,
                              ServiceDefinitionService serviceDefinitionService,
                              PackageService packageService,
                              PackageSessionService packageSessionService,
                              AppointmentValidationService validationService,
                              AppointmentStateService stateService,
                              ApplicationEventPublisher eventPublisher) {
        super(repository, "Randevu");
        this.businessService = businessService;
        this.customerService = customerService;
        this.employeeService = employeeService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.packageService = packageService;
        this.packageSessionService = packageSessionService;
        this.validationService = validationService;
        this.stateService = stateService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment createAppointment(Long businessId, CreateAppointmentRequest request) {
        Business business = businessService.findByIdOrThrow(businessId);
        customerService.findByIdOrThrow(request.customerId());
        employeeService.findByIdOrThrow(request.employeeId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(request.serviceId());

        validationService.validateCapability(request.employeeId(), request.serviceId());

        LocalDateTime endTime = request.scheduledAt().plusMinutes(service.totalDurationMinutes());
        validationService.validateSlotAvailability(businessId, request.employeeId(), request.scheduledAt(), endTime);
        validationService.validateDayLimit(business, request.customerId(), request.scheduledAt());

        Appointment appointment = buildAndPersist(businessId, request, service, business);
        tryAutoAssignPackageSession(appointment, request.serviceId());
        publishCreatedEvent(appointment);

        log.info("Randevu oluşturuldu: appointmentId={}, customerId={}, source={}",
            appointment.getId(), request.customerId(), request.getSource());
        return appointment;
    }

    @Transactional
    public Appointment createStaffAppointment(Long businessId, StaffAppointmentRequest request) {
        if (request.source() != AppointmentSource.PHONE && request.source() != AppointmentSource.WALK_IN) {
            throw new IllegalArgumentException(AppointmentErrorMessages.STAFF_SOURCE_INVALID);
        }
        var customer = customerService.findOrCreateByPhone(
            businessId, request.customerName(),
            request.getPhoneCountryCode(), request.phoneNumber());
        CreateAppointmentRequest internal = new CreateAppointmentRequest(
            customer.getId(), request.employeeId(), request.serviceId(),
            request.scheduledAt(), request.notes(), null, request.source());
        return createAppointment(businessId, internal);
    }

    @Transactional
    public Appointment updateStaffAppointment(
            Long businessId,
            Long appointmentId,
            UpdateStaffAppointmentRequest request) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        if (!appointment.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Bu randevu bu işletmeye ait değil");
        }
        if (!isStaffEditableStatus(appointment.getStatus())) {
            throw new ValidationException("APPOINTMENT_NOT_EDITABLE", "Bu durumdaki randevu düzenlenemez");
        }
        if (request.source() != null
                && request.source() != AppointmentSource.PHONE
                && request.source() != AppointmentSource.WALK_IN) {
            throw new IllegalArgumentException(AppointmentErrorMessages.STAFF_SOURCE_INVALID);
        }
        employeeService.findByIdOrThrow(request.employeeId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(request.serviceId());
        validationService.validateCapability(request.employeeId(), request.serviceId());
        int durationMinutes = service.totalDurationMinutes();
        LocalDateTime endTime = request.scheduledAt().plusMinutes(durationMinutes);
        validationService.validateSlotAvailabilityExcluding(
            businessId, request.employeeId(), request.scheduledAt(), endTime, appointmentId);
        Business business = businessService.findByIdOrThrow(businessId);
        validationService.validateDayLimitExcluding(
            business, appointment.getCustomerId(), request.scheduledAt(), appointmentId);
        if (appointment.getPackageSessionId() != null
                && !Objects.equals(appointment.getServiceId(), request.serviceId())) {
            unassignPackageSession(appointmentId);
            appointment = findByIdOrThrow(appointmentId);
        }
        appointment.applyStaffBookingUpdate(
            request.employeeId(),
            request.serviceId(),
            request.scheduledAt(),
            endTime,
            durationMinutes,
            request.notes(),
            request.source());
        Appointment saved = repository.save(appointment);
        log.info("Staff appointment updated: appointmentId={}, businessId={}", appointmentId, businessId);
        return saved;
    }

    private static boolean isStaffEditableStatus(AppointmentStatus status) {
        return status == AppointmentStatus.PENDING
                || status == AppointmentStatus.CONFIRMED
                || status == AppointmentStatus.RISKY
                || status == AppointmentStatus.RESCHEDULE_REQUESTED;
    }

    @Transactional
    public Appointment confirmAppointment(Long appointmentId) {
        return stateService.confirm(appointmentId);
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        return stateService.cancel(appointmentId, reason);
    }

    @Transactional
    public Appointment completeAppointment(Long appointmentId) {
        return stateService.complete(appointmentId);
    }

    @Transactional
    public Appointment markAsNoShow(Long appointmentId) {
        return stateService.markAsNoShow(appointmentId);
    }

    @Transactional
    public Appointment markAsRisky(Long appointmentId, String reason) {
        return stateService.markAsRisky(appointmentId, reason);
    }

    @Transactional
    public Appointment requestReschedule(Long appointmentId, String reason) {
        return stateService.requestReschedule(appointmentId, reason);
    }

    @Transactional
    public Appointment reassignAppointmentEmployee(Long appointmentId, Long newEmployeeId) {
        return stateService.reassignEmployee(appointmentId, newEmployeeId);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByBusinessIdPaged(Long businessId, Pageable pageable) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdOrderByScheduledAtDesc(businessId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByBusinessIdAndEmployeeIdPaged(Long businessId, Long employeeId, Pageable pageable) {
        businessService.findByIdOrThrow(businessId);
        employeeService.findByIdOrThrow(employeeId);
        return repository.findByBusinessIdAndEmployeeIdOrderByScheduledAtDesc(businessId, employeeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByBusinessIdAndCustomerIdPaged(Long businessId, Long customerId, Pageable pageable) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndCustomerIdOrderByScheduledAtDesc(businessId, customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByDateRangePaged(Long businessId, LocalDate startDate,
                                                   LocalDate endDate, Pageable pageable) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndDateRange(
            businessId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByEmployeeIdAndDateRangePaged(Long businessId, Long employeeId,
                                                                LocalDate startDate, LocalDate endDate,
                                                                Pageable pageable) {
        businessService.findByIdOrThrow(businessId);
        employeeService.findByIdOrThrow(employeeId);
        return repository.findByBusinessIdAndEmployeeIdAndDateRange(
            businessId, employeeId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable);
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
            businessId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByEmployeeAndDateRange(Long businessId, Long employeeId, LocalDate date) {
        return repository.findByBusinessIdAndEmployeeIdAndScheduledAtBetween(
            businessId, employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
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
            throw new IllegalArgumentException(AppointmentErrorMessages.NO_LINKED_SESSION);
        }
        packageService.unassignSessionFromAppointment(appointment.getPackageSessionId());
        appointment.setPackageSessionId(null);
        return repository.save(appointment);
    }

    @Override
    public Appointment findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    private Appointment buildAndPersist(Long businessId, CreateAppointmentRequest request,
                                         ServiceDefinition service, Business business) {
        Appointment appointment = AppointmentMapper.toEntity(businessId, request, service.totalDurationMinutes());
        var customer = customerService.findByIdOrThrow(request.customerId());
        boolean forcePending = customer.isNoShowForcesManualApproval();
        if (!forcePending && business.isAutoConfirm() && !service.isRequiresConfirmation()) {
            appointment.confirm();
            log.info("Randevu otomatik onaylandı: businessId={}", businessId);
        }
        return repository.save(appointment);
    }

    private void tryAutoAssignPackageSession(Appointment appointment, Long serviceId) {
        packageService.findActivePackageForService(appointment.getCustomerId(), serviceId)
            .flatMap(pkg -> packageSessionService.findFirstPendingSession(pkg.getId()))
            .ifPresent(session -> {
                packageSessionService.assignAppointment(
                    session.getId(), appointment.getId(), appointment.getScheduledAt());
                appointment.setPackageSessionId(session.getId());
                repository.save(appointment);
                log.info("Paketten seans otomatik atandı: appointmentId={}, sessionId={}",
                    appointment.getId(), session.getId());
            });
    }

    private void publishCreatedEvent(Appointment saved) {
        eventPublisher.publishEvent(new AppointmentCreated(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(), saved.getEmployeeId(),
            saved.getServiceId(), saved.getScheduledAt(), saved.getEndAt(), saved.getStatus().name()));
    }
}
