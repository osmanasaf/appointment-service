package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.common.util.PhoneNumberUtil;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.notification.NotificationLog;
import com.asaf.appointment_service.entity.notification.NotificationStatus;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.integration.sms.SmsClient;
import com.asaf.appointment_service.integration.sms.SmsSendResult;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageRequest;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final WhatsAppClientInterface whatsAppClient;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final CustomerService customerService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final BusinessService businessService;
    private final SmsClient smsClient;

    @Value("${app.sms.enabled:false}")
    private boolean smsFallbackEnabled;

    public NotificationService(WhatsAppClientInterface whatsAppClient,
                               NotificationLogRepository notificationLogRepository,
                               NotificationSettingsService notificationSettingsService,
                               CustomerService customerService,
                               ServiceDefinitionService serviceDefinitionService,
                               BusinessService businessService,
                               SmsClient smsClient) {
        this.whatsAppClient = whatsAppClient;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.customerService = customerService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.businessService = businessService;
        this.smsClient = smsClient;
    }

    @Transactional
    public void sendAppointmentReminder(Appointment appointment) {
        if (notificationLogRepository.existsByAppointmentIdAndType(
                appointment.getId(), NotificationType.APPOINTMENT_REMINDER)) {
            log.debug("Hatırlatma zaten gönderilmiş: appointmentId={}", appointment.getId());
            return;
        }

        Customer customer = customerService.findByIdOrThrow(appointment.getCustomerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        String priceText = formatPriceIfEnabled(appointment.getBusinessId(), service);

        WhatsAppMessageRequest messageRequest = WhatsAppMessageRequest.reminder(
            customer.getFormattedPhoneNumber(),
            customer.getName(),
            service.getName(),
            appointment.getScheduledAt().format(DATE_FORMATTER),
            appointment.getScheduledAt().format(TIME_FORMATTER),
            priceText
        );

        sendAndLog(appointment.getBusinessId(), appointment.getId(), customer,
            NotificationType.APPOINTMENT_REMINDER, messageRequest);
    }

    @Transactional
    public void sendConfirmationRequest(Appointment appointment, String confirmLink) {
        Customer customer = customerService.findByIdOrThrow(appointment.getCustomerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        String priceText = formatPriceIfEnabled(appointment.getBusinessId(), service);

        WhatsAppMessageRequest messageRequest = WhatsAppMessageRequest.confirmation(
            customer.getFormattedPhoneNumber(),
            customer.getName(),
            service.getName(),
            appointment.getScheduledAt().format(DATE_FORMATTER),
            appointment.getScheduledAt().format(TIME_FORMATTER),
            confirmLink,
            priceText
        );

        sendAndLog(appointment.getBusinessId(), appointment.getId(), customer,
            NotificationType.CONFIRMATION_REQUEST, messageRequest);
    }

    @Transactional
    public void sendCancellationNotice(Appointment appointment) {
        Customer customer = customerService.findByIdOrThrow(appointment.getCustomerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        String priceText = formatPriceIfEnabled(appointment.getBusinessId(), service);

        WhatsAppMessageRequest messageRequest = WhatsAppMessageRequest.cancelled(
            customer.getFormattedPhoneNumber(),
            customer.getName(),
            service.getName(),
            appointment.getScheduledAt().format(DATE_FORMATTER),
            priceText
        );

        sendAndLog(appointment.getBusinessId(), appointment.getId(), customer,
            NotificationType.APPOINTMENT_CANCELLED, messageRequest);
    }

    @Transactional
    public void sendRescheduleConfirmation(Appointment appointment) {
        Customer customer = customerService.findByIdOrThrow(appointment.getCustomerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());
        String priceText = formatPriceIfEnabled(appointment.getBusinessId(), service);

        WhatsAppMessageRequest messageRequest = WhatsAppMessageRequest.rescheduled(
            customer.getFormattedPhoneNumber(),
            customer.getName(),
            service.getName(),
            appointment.getScheduledAt().format(DATE_FORMATTER),
            appointment.getScheduledAt().format(TIME_FORMATTER),
            priceText
        );

        sendAndLog(appointment.getBusinessId(), appointment.getId(), customer,
            NotificationType.APPOINTMENT_RESCHEDULED, messageRequest);
    }

    /**
     * Bekleme listesindeki müşteriye, boşalan bir slotu WhatsApp üzerinden teklif eder.
     * Henüz bir randevu oluşmadığı için appointmentId taşımaz.
     */
    @Transactional
    public void sendWaitlistSlotOffer(Long businessId, Customer customer, ServiceDefinition service,
                                      LocalDateTime offeredScheduledAt, String offerLink) {
        String priceText = formatPriceIfEnabled(businessId, service);
        WhatsAppMessageRequest messageRequest = WhatsAppMessageRequest.waitlistSlotOffer(
            customer.getFormattedPhoneNumber(),
            customer.getName(),
            service.getName(),
            offeredScheduledAt.format(DATE_FORMATTER),
            offeredScheduledAt.format(TIME_FORMATTER),
            offerLink,
            priceText
        );

        sendAndLog(businessId, null, customer, NotificationType.WAITLIST_SLOT_OFFER, messageRequest);
    }

    @Transactional
    public void retryFailedNotifications() {
        List<NotificationLog> failedLogs = notificationLogRepository
            .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        for (NotificationLog logEntry : failedLogs) {
            if (logEntry.canRetry()) {
                retryNotification(logEntry);
            }
        }
    }

    private void sendAndLog(Long businessId, Long appointmentId, Customer customer,
                            NotificationType type, WhatsAppMessageRequest messageRequest) {
        NotificationLog logEntry = buildLogEntry(businessId, appointmentId, customer, type, messageRequest);
        WhatsAppMessageResponse response = whatsAppClient.sendMessage(messageRequest);
        if (response.success()) {
            logEntry.markAsSent(response.messageId());
            notificationSettingsService.incrementWhatsappUsage(businessId);
            log.info("Bildirim gönderildi: type={}, appointmentId={}", type, appointmentId);
        } else {
            logEntry.markAsFailed(response.errorMessage());
            log.error("Bildirim gönderilemedi: type={}, error={}", type, response.errorMessage());
            trySmsIfFallback(customer, type, messageRequest, appointmentId);
        }
        notificationLogRepository.save(logEntry);
    }

    private NotificationLog buildLogEntry(Long businessId, Long appointmentId, Customer customer,
                                          NotificationType type, WhatsAppMessageRequest messageRequest) {
        return NotificationLog.builder()
            .businessId(businessId)
            .customerId(customer.getId())
            .appointmentId(appointmentId)
            .type(type)
            .phoneNumber(PhoneNumberUtil.normalizeDigitsOnly(customer.getFormattedPhoneNumber()))
            .message(messageRequest.templateName() + ": " + messageRequest.parameters())
            .build();
    }

    private void trySmsIfFallback(Customer customer, NotificationType type,
                                   WhatsAppMessageRequest messageRequest, Long appointmentId) {
        if (!smsFallbackEnabled) {
            return;
        }
        String smsText = buildSmsFallbackText(type, messageRequest);
        SmsSendResult smsResult = smsClient.send(customer.getFormattedPhoneNumber(), smsText);
        if (smsResult.success()) {
            log.info("SMS fallback gönderildi: type={}, appointmentId={}", type, appointmentId);
        } else {
            log.warn("SMS fallback başarısız: type={}, error={}", type, smsResult.errorMessage());
        }
    }

    private String buildSmsFallbackText(NotificationType type, WhatsAppMessageRequest messageRequest) {
        var params = messageRequest.parameters();
        String name = params.getOrDefault("customer_name", "");
        String serviceName = params.getOrDefault("service_name", "");
        String date = params.getOrDefault("date", "");
        String time = params.getOrDefault("time", "");
        return switch (type) {
            case APPOINTMENT_REMINDER -> "Randevu hatırlatma: %s, %s, %s %s.".formatted(name, serviceName, date, time);
            case CONFIRMATION_REQUEST -> "Randevu onayı bekleniyor: %s, %s, %s %s.".formatted(name, serviceName, date, time);
            case APPOINTMENT_CANCELLED -> "Randevu iptal edildi: %s, %s %s.".formatted(name, serviceName, date);
            default -> "Bildirim: %s %s.".formatted(date, time);
        };
    }

    private void retryNotification(NotificationLog logEntry) {
        log.info("Bildirim tekrar deneniyor: id={}, attempt={}", logEntry.getId(), logEntry.getRetryCount() + 1);
    }

    private String formatPriceIfEnabled(Long businessId, ServiceDefinition service) {
        if (service.getPrice() == null) return null;
        var business = businessService.findByIdOrThrow(businessId);
        if (!business.isWhatsappSendPrice()) return null;
        String amount = service.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        String currency = service.getCurrency() != null ? service.getCurrency() : "TRY";
        return amount + " " + currency;
    }
}
