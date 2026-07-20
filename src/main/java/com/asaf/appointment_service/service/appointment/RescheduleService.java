package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.dto.appointment.RescheduleContextResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Müşterinin WhatsApp üzerinden aldığı tek kullanımlık link ile randevusunu
 * panele girmeden kendi başına yeni bir saate taşıması (self-servis erteleme).
 */
@Service
public class RescheduleService {

    private static final Logger log = LoggerFactory.getLogger(RescheduleService.class);

    private final PublicActionTokenService tokenService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentValidationService validationService;
    private final BusinessService businessService;
    private final EmployeeService employeeService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final NotificationService notificationService;

    public RescheduleService(PublicActionTokenService tokenService,
                             AppointmentRepository appointmentRepository,
                             AppointmentValidationService validationService,
                             BusinessService businessService,
                             EmployeeService employeeService,
                             ServiceDefinitionService serviceDefinitionService,
                             NotificationService notificationService) {
        this.tokenService = tokenService;
        this.appointmentRepository = appointmentRepository;
        this.validationService = validationService;
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public RescheduleContextResponse getContext(String token) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.RESCHEDULE);
        Appointment appointment = findAppointmentOrThrow(actionToken.getAppointmentId());
        Business business = businessService.findByIdOrThrow(appointment.getBusinessId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        Employee employee = employeeService.findByIdOrThrow(appointment.getEmployeeId());

        return new RescheduleContextResponse(
            appointment.getId(),
            business.getSlug(),
            business.getName(),
            service.getId(),
            service.getName(),
            employee.getId(),
            employee.getName(),
            appointment.getScheduledAt(),
            appointment.getDurationMinutes(),
            appointment.getStatus()
        );
    }

    @Transactional
    public Appointment reschedule(String token, Long employeeId, LocalDateTime newScheduledAt) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.RESCHEDULE);
        Appointment appointment = findAppointmentOrThrow(actionToken.getAppointmentId());
        RescheduleTarget target = resolveAndValidateTarget(appointment, employeeId, newScheduledAt);

        appointment.reschedule(employeeId, newScheduledAt, target.newEndAt(), target.durationMinutes(),
            target.autoConfirm());
        Appointment saved = appointmentRepository.save(appointment);

        tokenService.consume(actionToken);
        notifyCustomerSafely(saved);

        return saved;
    }

    private RescheduleTarget resolveAndValidateTarget(Appointment appointment, Long employeeId,
                                                        LocalDateTime newScheduledAt) {
        if (appointment.getStatus() != AppointmentStatus.RESCHEDULE_REQUESTED) {
            throw new BusinessException("APPOINTMENT_NOT_RESCHEDULABLE",
                "Bu randevu artık ertelenebilir durumda değil.");
        }

        Business business = businessService.findByIdOrThrow(appointment.getBusinessId());
        Employee employee = employeeService.findByIdOrThrow(employeeId);
        if (!employee.getBusinessId().equals(appointment.getBusinessId())) {
            throw new BusinessException("EMPLOYEE_NOT_IN_BUSINESS", "Seçilen çalışan bu işletmeye ait değil.");
        }
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        validationService.validateCapability(employeeId, appointment.getServiceId());
        validationService.validateBookingPolicy(business, newScheduledAt.toLocalDate(), service);

        int durationMinutes = service.totalDurationMinutes();
        LocalDateTime newEndAt = newScheduledAt.plusMinutes(durationMinutes);
        validationService.validateSlotAvailabilityExcluding(
            appointment.getBusinessId(), employeeId, newScheduledAt, newEndAt, appointment.getId());
        validationService.validateDayLimitExcluding(
            business, appointment.getCustomerId(), newScheduledAt, appointment.getId());

        boolean autoConfirm = business.isAutoConfirm() && !service.isRequiresConfirmation();
        return new RescheduleTarget(newEndAt, durationMinutes, autoConfirm);
    }

    private void notifyCustomerSafely(Appointment appointment) {
        try {
            notificationService.sendRescheduleConfirmation(appointment);
        } catch (Exception e) {
            log.warn("Erteleme onay bildirimi gönderilemedi: appointmentId={}, error={}",
                appointment.getId(), e.getMessage());
        }
    }

    private Appointment findAppointmentOrThrow(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private record RescheduleTarget(LocalDateTime newEndAt, int durationMinutes, boolean autoConfirm) {
    }
}
