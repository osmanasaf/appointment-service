package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.calendar.IcsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final BusinessService businessService;
    private final NotificationService notificationService;
    private final IcsService icsService;
    private final EmailProperties emailProperties;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                             BusinessService businessService,
                             NotificationService notificationService,
                             IcsService icsService,
                             EmailProperties emailProperties) {
        this.appointmentRepository = appointmentRepository;
        this.businessService = businessService;
        this.notificationService = notificationService;
        this.icsService = icsService;
        this.emailProperties = emailProperties;
    }

    @Scheduled(fixedDelayString = "${reminder.scheduler.interval:3600000}")
    public void sendScheduledReminders() {
        log.info("Hatırlatma scheduler çalışıyor...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(25);

        List<Appointment> appointments = appointmentRepository
            .findAppointmentsNeedingReminder(now, reminderWindowEnd);

        if (appointments.isEmpty()) {
            log.info("Hatırlatma scheduler tamamlandı: 0 hatırlatma gönderildi");
            return;
        }

        Map<Long, Business> businessCache = buildBusinessCache(appointments);
        int sentCount = 0;

        for (Appointment appointment : appointments) {
            try {
                Business business = businessCache.get(appointment.getBusinessId());
                if (business != null && appointment.needsReminder(business.getReminderHoursBefore())) {
                    String calendarLink = icsService.getOrIssueCalendarLink(emailProperties.baseUrl(), appointment);
                    notificationService.sendAppointmentReminder(appointment, calendarLink);
                    appointment.markReminderSent();
                    appointmentRepository.save(appointment);
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Hatırlatma gönderilemedi: appointmentId={}, error={}",
                    appointment.getId(), e.getMessage());
            }
        }

        log.info("Hatırlatma scheduler tamamlandı: {} hatırlatma gönderildi", sentCount);
    }

    @Scheduled(cron = "${reminder.retry.cron:0 */15 * * * *}")
    public void retryFailedReminders() {
        log.info("Başarısız bildirimler tekrar deneniyor...");
        notificationService.retryFailedNotifications();
    }

    private Map<Long, Business> buildBusinessCache(List<Appointment> appointments) {
        List<Long> businessIds = appointments.stream()
            .map(Appointment::getBusinessId)
            .distinct()
            .toList();
        return businessService.findAllByIds(businessIds).stream()
            .collect(Collectors.toMap(Business::getId, b -> b));
    }
}
