package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.domain.event.AppointmentCancelled;
import com.asaf.appointment_service.domain.event.AppointmentCreated;
import com.asaf.appointment_service.domain.event.AppointmentRescheduledEvent;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.dto.GoogleCalendarEventData;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;

/**
 * Randevu yaşam döngüsü olaylarını (oluşturma/erteleme/iptal) dinleyip bağlı çalışanın
 * kişisel Google Takvimi'ne yansıtır (dışa akış). Çalışanın takvim bağlantısı yoksa veya
 * BROKEN durumdaysa sessizce atlanır — senkron başarısızlığı randevu akışını etkilemez.
 */
@Service
public class GoogleCalendarSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarSyncService.class);
    private static final String EVENT_DESCRIPTION = "Randevum üzerinden oluşturuldu";

    private final CalendarConnectionRepository connectionRepository;
    private final CalendarConnectionService connectionService;
    private final GoogleCalendarClient googleCalendarClient;
    private final AppointmentRepository appointmentRepository;
    private final ServiceDefinitionService serviceDefinitionService;
    private final CustomerService customerService;

    public GoogleCalendarSyncService(CalendarConnectionRepository connectionRepository,
                                      CalendarConnectionService connectionService,
                                      GoogleCalendarClient googleCalendarClient,
                                      AppointmentRepository appointmentRepository,
                                      ServiceDefinitionService serviceDefinitionService,
                                      CustomerService customerService) {
        this.connectionRepository = connectionRepository;
        this.connectionService = connectionService;
        this.googleCalendarClient = googleCalendarClient;
        this.appointmentRepository = appointmentRepository;
        this.serviceDefinitionService = serviceDefinitionService;
        this.customerService = customerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentCreated(AppointmentCreated event) {
        try {
            upsert(event.appointmentId(), event.employeeId());
        } catch (Exception e) {
            log.error("Google Takvim senkronu başarısız (oluşturma): appointmentId={}, error={}",
                event.appointmentId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        try {
            if (!event.oldEmployeeId().equals(event.newEmployeeId())) {
                deleteFromEmployeeCalendar(event.appointmentId(), event.oldEmployeeId());
            }
            upsert(event.appointmentId(), event.newEmployeeId());
        } catch (Exception e) {
            log.error("Google Takvim senkronu başarısız (erteleme): appointmentId={}, error={}",
                event.appointmentId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentCancelled(AppointmentCancelled event) {
        try {
            Appointment appointment = appointmentRepository.findById(event.appointmentId()).orElse(null);
            if (appointment != null) {
                deleteFromEmployeeCalendar(event.appointmentId(), appointment.getEmployeeId());
            }
        } catch (Exception e) {
            log.error("Google Takvim senkronu başarısız (iptal): appointmentId={}, error={}",
                event.appointmentId(), e.getMessage());
        }
    }

    private void upsert(Long appointmentId, Long employeeId) {
        Optional<CalendarConnection> maybeConnection = activeConnection(employeeId);
        if (maybeConnection.isEmpty()) {
            return;
        }
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return;
        }
        CalendarConnection connection = maybeConnection.get();
        String accessToken = connectionService.resolveAccessToken(connection);
        if (accessToken == null) {
            return;
        }
        GoogleCalendarEventData eventData = buildEventData(appointment);
        String eventId = googleCalendarClient.upsertEvent(
            accessToken, connection.getGoogleCalendarId(), appointment.getGoogleEventId(), eventData);
        appointment.linkGoogleEvent(eventId);
        appointmentRepository.save(appointment);
    }

    private void deleteFromEmployeeCalendar(Long appointmentId, Long employeeId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null || appointment.getGoogleEventId() == null) {
            return;
        }
        Optional<CalendarConnection> maybeConnection = activeConnection(employeeId);
        if (maybeConnection.isEmpty()) {
            return;
        }
        CalendarConnection connection = maybeConnection.get();
        String accessToken = connectionService.resolveAccessToken(connection);
        if (accessToken == null) {
            return;
        }
        googleCalendarClient.deleteEvent(accessToken, connection.getGoogleCalendarId(), appointment.getGoogleEventId());
        appointment.unlinkGoogleEvent();
        appointmentRepository.save(appointment);
    }

    private Optional<CalendarConnection> activeConnection(Long employeeId) {
        return connectionRepository.findByEmployeeId(employeeId)
            .filter(c -> c.getStatus() == CalendarConnectionStatus.CONNECTED);
    }

    private GoogleCalendarEventData buildEventData(Appointment appointment) {
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        Customer customer = customerService.findByIdOrThrow(appointment.getCustomerId());
        String summary = "%s - %s".formatted(service.getName(), customer.getName());
        return new GoogleCalendarEventData(
            summary, EVENT_DESCRIPTION, appointment.getScheduledAt(), appointment.getEndAt(), "Europe/Istanbul");
    }
}
