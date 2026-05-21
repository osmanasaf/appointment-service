package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.domain.event.AppointmentCancelled;
import com.asaf.appointment_service.domain.event.AppointmentCompleted;
import com.asaf.appointment_service.domain.event.AppointmentConfirmed;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentStateService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentStateService.class);

    private final AppointmentRepository appointmentRepository;
    private final PackageSessionService packageSessionService;
    private final PackageService packageService;
    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentStateService(AppointmentRepository appointmentRepository,
                                   PackageSessionService packageSessionService,
                                   PackageService packageService,
                                   CustomerService customerService,
                                   ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.packageSessionService = packageSessionService;
        this.packageService = packageService;
        this.customerService = customerService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment confirm(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.confirm();
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentConfirmed(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(),
            saved.getScheduledAt(), saved.getConfirmedAt()));
        log.info("Randevu onaylandı: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment cancel(Long appointmentId, String reason) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        unassignSessionIfPresent(appointment);
        appointment.cancel(reason);
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentCancelled(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(),
            saved.getScheduledAt(), saved.getCancellationReason()));
        log.info("Randevu iptal edildi: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment complete(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.complete();
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getPackageSessionId() != null) {
            packageSessionService.markCompleted(saved.getPackageSessionId());
        } else {
            deductSessionIfEligible(appointment);
        }
        eventPublisher.publishEvent(new AppointmentCompleted(
            saved.getId(), saved.getBusinessId(), saved.getCustomerId(), saved.getServiceId(),
            saved.getScheduledAt(), saved.getCompletedAt()));
        log.info("Randevu tamamlandı: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment markAsNoShow(Long appointmentId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.markAsNoShow();
        if (appointment.getPackageSessionId() != null) {
            packageSessionService.markNoShow(appointment.getPackageSessionId());
        }
        customerService.recordNoShow(appointment.getCustomerId());
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Randevu no-show işaretlendi: appointmentId={}, customerId={}",
            appointmentId, appointment.getCustomerId());
        return saved;
    }

    @Transactional
    public Appointment markAsRisky(Long appointmentId, String reason) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.markAsRisky(reason);
        Appointment saved = appointmentRepository.save(appointment);
        log.warn("Randevu riskli işaretlendi: appointmentId={}", appointmentId);
        return saved;
    }

    @Transactional
    public Appointment reassignEmployee(Long appointmentId, Long newEmployeeId) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.reassignEmployee(newEmployeeId);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Randevu yeniden atandı: appointmentId={}, newEmployeeId={}", appointmentId, newEmployeeId);
        return saved;
    }

    @Transactional
    public Appointment requestReschedule(Long appointmentId, String reason) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        appointment.requestReschedule(reason);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Randevu erteleme talep edildi: appointmentId={}", appointmentId);
        return saved;
    }

    private void unassignSessionIfPresent(Appointment appointment) {
        if (appointment.getPackageSessionId() != null) {
            packageSessionService.unassignAppointment(appointment.getPackageSessionId());
            appointment.setPackageSessionId(null);
        }
    }

    private void deductSessionIfEligible(Appointment appointment) {
        try {
            packageService.deductSessionForAppointment(appointment.getCustomerId(), appointment.getServiceId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Randevu tamamlandı ancak paket seans düşülemedi: customerId={}, serviceId={}, error={}",
                appointment.getCustomerId(), appointment.getServiceId(), e.getMessage());
        }
    }

    public Appointment findByIdOrThrow(Long id) {
        return appointmentRepository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));
    }
}
