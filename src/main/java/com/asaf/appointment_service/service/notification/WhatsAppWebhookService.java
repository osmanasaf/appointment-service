package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.common.util.PhoneNumberUtil;
import com.asaf.appointment_service.entity.notification.NotificationLog;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);
    private static final int REMINDER_LOOKBACK_HOURS = 48;

    private static final List<String> CONFIRM_KEYWORDS = List.of(
        "geliyorum", "onaylıyorum", "onay", "1", "evet", "ok", "tamam", "kabul", "confirm", "yes",
        "confirm_appointment"
    );
    private static final List<String> CANCEL_KEYWORDS = List.of(
        "iptal", "iptal ediyorum", "2", "hayır", "gelemeyeceğim", "gelemem", "cancel", "no",
        "cancel_appointment"
    );
    private static final List<String> RESCHEDULE_KEYWORDS = List.of(
        "ertele", "erteleme", "3", "başka", "değiştir", "reschedule",
        "reschedule_appointment"
    );

    private static final String AUTO_REPLY_UNKNOWN = """
        Mesajınızı anlayamadım.
        
        Randevunuzu onaylamak için "Geliyorum" yazın.
        İptal etmek için "İptal" yazın.
        Ertelemek için "Ertele" yazın.
        
        Randevum Pro""";

    private static final String AUTO_REPLY_CONFIRMED = """
        ✅ Randevunuz onaylandı!
        
        Görüşmek üzere.
        
        Randevum Pro""";

    private static final String AUTO_REPLY_CANCELLED = """
        ❌ Randevunuz iptal edildi.
        
        Yeni randevu almak için web sitemizi ziyaret edebilirsiniz.
        
        Randevum Pro""";

    private static final String AUTO_REPLY_RESCHEDULE_REQUESTED = """
        🔄 Erteleme talebiniz alındı!
        
        En kısa sürede sizinle iletişime geçeceğiz.
        
        Randevum Pro""";

    private static final String AUTO_REPLY_NO_APPOINTMENT = """
        Aktif bir randevunuz bulunamadı.
        
        Yeni randevu almak için web sitemizi ziyaret edebilirsiniz.
        
        Randevum Pro""";

    private final NotificationLogRepository notificationLogRepository;
    private final AppointmentService appointmentService;
    private final WhatsAppClientInterface whatsAppClient;

    public WhatsAppWebhookService(NotificationLogRepository notificationLogRepository,
                                  AppointmentService appointmentService,
                                  WhatsAppClientInterface whatsAppClient) {
        this.notificationLogRepository = notificationLogRepository;
        this.appointmentService = appointmentService;
        this.whatsAppClient = whatsAppClient;
    }

    @Transactional
    public void processWebhookPayload(Map<String, Object> payload) {
        if (payload == null) return;
        List<?> entries = getList(payload, "entry");
        if (entries == null) return;
        for (Object entryObj : entries) {
            if (!(entryObj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) entryObj;
            List<?> changes = getList(entry, "changes");
            if (changes == null) continue;
            for (Object changeObj : changes) {
                if (!(changeObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> change = (Map<String, Object>) changeObj;
                Object valueObj = change.get("value");
                if (!(valueObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) valueObj;
                processMessages(value);
            }
        }
    }

    private void processMessages(Map<String, Object> value) {
        List<?> messages = getList(value, "messages");
        if (messages == null) return;
        for (Object msgObj : messages) {
            if (!(msgObj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = (Map<String, Object>) msgObj;
            String from = getString(msg, "from");
            if (from == null) continue;
            String text = extractMessageText(msg);
            if (text == null || text.isBlank()) continue;
            handleIncomingMessage(normalizePhone(from), text.trim());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageText(Map<String, Object> msg) {
        Object textObj = msg.get("text");
        if (textObj instanceof Map) {
            String body = getString((Map<String, Object>) textObj, "body");
            if (body != null) return body;
        }
        Object interactiveObj = msg.get("interactive");
        if (interactiveObj instanceof Map) {
            Object buttonReply = ((Map<String, Object>) interactiveObj).get("button_reply");
            if (buttonReply instanceof Map) {
                String title = getString((Map<String, Object>) buttonReply, "title");
                if (title != null) return title;
                String id = getString((Map<String, Object>) buttonReply, "id");
                if (id != null) return id;
            }
        }
        return null;
    }

    private void handleIncomingMessage(String normalizedPhone, String text) {
        String lower = text.toLowerCase().trim();
        
        if (CONFIRM_KEYWORDS.stream().anyMatch(lower::contains)) {
            confirmAppointment(normalizedPhone);
            return;
        }
        
        if (CANCEL_KEYWORDS.stream().anyMatch(lower::contains)) {
            cancelAppointment(normalizedPhone);
            return;
        }
        
        if (RESCHEDULE_KEYWORDS.stream().anyMatch(lower::contains)) {
            requestReschedule(normalizedPhone);
            return;
        }
        
        sendAutoReply(normalizedPhone, AUTO_REPLY_UNKNOWN);
        log.info("Bilinmeyen mesaj alındı, auto-reply gönderildi: telefon={}", 
            PhoneNumberUtil.maskForLog(normalizedPhone));
    }

    private void confirmAppointment(String normalizedPhone) {
        Optional<Long> appointmentId = resolveAppointmentId(normalizedPhone);
        if (appointmentId.isEmpty()) {
            log.warn("WhatsApp onay: randevu bulunamadı, telefon={}", PhoneNumberUtil.maskForLog(normalizedPhone));
            sendAutoReply(normalizedPhone, AUTO_REPLY_NO_APPOINTMENT);
            return;
        }
        try {
            appointmentService.confirmAppointment(appointmentId.get());
            log.info("WhatsApp ile randevu onaylandı: appointmentId={}", appointmentId.get());
            sendAutoReply(normalizedPhone, AUTO_REPLY_CONFIRMED);
        } catch (Exception e) {
            log.error("WhatsApp onay işlenemedi: appointmentId={}, error={}", appointmentId.get(), e.getMessage());
        }
    }

    private void cancelAppointment(String normalizedPhone) {
        Optional<Long> appointmentId = resolveAppointmentId(normalizedPhone);
        if (appointmentId.isEmpty()) {
            log.warn("WhatsApp iptal: randevu bulunamadı, telefon={}", PhoneNumberUtil.maskForLog(normalizedPhone));
            sendAutoReply(normalizedPhone, AUTO_REPLY_NO_APPOINTMENT);
            return;
        }
        try {
            appointmentService.cancelAppointment(appointmentId.get(), "WhatsApp üzerinden iptal");
            log.info("WhatsApp ile randevu iptal edildi: appointmentId={}", appointmentId.get());
            sendAutoReply(normalizedPhone, AUTO_REPLY_CANCELLED);
        } catch (Exception e) {
            log.error("WhatsApp iptal işlenemedi: appointmentId={}, error={}", appointmentId.get(), e.getMessage());
        }
    }

    private void requestReschedule(String normalizedPhone) {
        Optional<Long> appointmentId = resolveAppointmentId(normalizedPhone);
        if (appointmentId.isEmpty()) {
            log.warn("WhatsApp erteleme: randevu bulunamadı, telefon={}", PhoneNumberUtil.maskForLog(normalizedPhone));
            sendAutoReply(normalizedPhone, AUTO_REPLY_NO_APPOINTMENT);
            return;
        }
        try {
            appointmentService.requestReschedule(appointmentId.get(), "WhatsApp üzerinden erteleme talebi");
            log.info("WhatsApp ile erteleme talep edildi: appointmentId={}", appointmentId.get());
            sendAutoReply(normalizedPhone, AUTO_REPLY_RESCHEDULE_REQUESTED);
        } catch (Exception e) {
            log.error("WhatsApp erteleme işlenemedi: appointmentId={}, error={}", appointmentId.get(), e.getMessage());
        }
    }

    private void sendAutoReply(String normalizedPhone, String message) {
        if (!whatsAppClient.isEnabled()) {
            log.debug("WhatsApp devre dışı, auto-reply gönderilmedi");
            return;
        }
        try {
            whatsAppClient.sendTextMessage(normalizedPhone, message);
        } catch (Exception e) {
            log.warn("Auto-reply gönderilemedi: {}", e.getMessage());
        }
    }

    Optional<Long> resolveAppointmentId(String normalizedPhone) {
        LocalDateTime after = LocalDateTime.now().minusHours(REMINDER_LOOKBACK_HOURS);
        List<NotificationType> types = List.of(
            NotificationType.APPOINTMENT_REMINDER,
            NotificationType.CONFIRMATION_REQUEST
        );
        Optional<NotificationLog> logEntry = notificationLogRepository
            .findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                normalizedPhone, types, after);
        return logEntry
            .filter(log -> log.getAppointmentId() != null)
            .map(NotificationLog::getAppointmentId);
    }

    static String normalizePhone(String phone) {
        return PhoneNumberUtil.normalizeDigitsOnly(phone);
    }

    @SuppressWarnings("unchecked")
    private static List<?> getList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof List ? (List<?>) v : null;
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
