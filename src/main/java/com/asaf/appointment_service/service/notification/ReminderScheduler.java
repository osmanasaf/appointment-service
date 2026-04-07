package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler for sending T-24 hour appointment reminders.
 */
@Service
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final BusinessService businessService;
    private final NotificationService notificationService;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                             BusinessService businessService,
                             NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.businessService = businessService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${reminder.scheduler.interval:3600000}")
    public void sendScheduledReminders() {
        log.info("Hatırlatma scheduler çalışıyor...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(25);

        List<Appointment> appointmentsNeedingReminder = appointmentRepository
            .findAppointmentsNeedingReminder(now, reminderWindowEnd);

        int sentCount = 0;
        for (Appointment appointment : appointmentsNeedingReminder) {
            try {
                Business business = businessService.findByIdOrThrow(appointment.getBusinessId());
                
                if (appointment.needsReminder(business.getReminderHoursBefore())) {
                    notificationService.sendAppointmentReminder(appointment);
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
}
