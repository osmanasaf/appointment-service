package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.domain.event.SlotFreedEvent;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bir randevu iptal/erteleme ile boşaldığında (SlotFreedEvent), bekleme
 * listesindeki en eski uygun kayda otomatik WhatsApp teklifi gönderir.
 */
@Service
public class WaitlistMatchingService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistMatchingService.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final PublicActionTokenService tokenService;
    private final NotificationService notificationService;
    private final NotificationSettingsService notificationSettingsService;
    private final CustomerService customerService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final EmailProperties emailProperties;

    public WaitlistMatchingService(WaitlistEntryRepository waitlistEntryRepository,
                                   PublicActionTokenService tokenService,
                                   NotificationService notificationService,
                                   NotificationSettingsService notificationSettingsService,
                                   CustomerService customerService,
                                   ServiceDefinitionService serviceDefinitionService,
                                   EmailProperties emailProperties) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.notificationSettingsService = notificationSettingsService;
        this.customerService = customerService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.emailProperties = emailProperties;
    }

    /**
     * Boşalan slotun transaction'ı kesin olarak commit edildikten sonra çalışır;
     * aksi halde henüz rollback edilebilecek bir iptali "boş slot" sayıp teklif göndermiş oluruz.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSlotFreed(SlotFreedEvent event) {
        try {
            offerToNextCandidate(event);
        } catch (Exception e) {
            log.error("Waitlist eşleştirme başarısız: businessId={}, error={}", event.businessId(), e.getMessage());
        }
    }

    private void offerToNextCandidate(SlotFreedEvent event) {
        List<WaitlistEntry> candidates = waitlistEntryRepository
            .findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(
                event.businessId(), event.serviceId(), WaitlistStatus.WAITING);

        Optional<WaitlistEntry> match = candidates.stream()
            .filter(entry -> entry.matchesFreedSlot(event.serviceId(), event.employeeId(), event.scheduledAt()))
            .findFirst();

        if (match.isEmpty()) {
            log.debug("Waitlist eşleşmesi bulunamadı: businessId={}, serviceId={}",
                event.businessId(), event.serviceId());
            return;
        }

        if (notificationSettingsService.getRemainingWhatsappQuota(event.businessId()) == 0) {
            log.warn("WhatsApp kotası dolu, waitlist teklifi atlandı: businessId={}, waitlistEntryId={}",
                event.businessId(), match.get().getId());
            return;
        }

        claimAndSendOffer(match.get().getId(), event);
    }

    /**
     * Eşzamanlı bir başka SlotFreedEvent aynı kaydı bu sırada NOTIFIED'a taşımış olabilir;
     * satırı kilitleyip durumu yeniden doğrulamadan teklif göndermeyiz.
     */
    private void claimAndSendOffer(Long entryId, SlotFreedEvent event) {
        WaitlistEntry locked = waitlistEntryRepository.findByIdForUpdate(entryId).orElse(null);
        if (locked == null || locked.getStatus() != WaitlistStatus.WAITING) {
            log.debug("Waitlist kaydı başka bir işlem tarafından ele alındı, atlanıyor: waitlistEntryId={}", entryId);
            return;
        }
        sendOffer(locked, event);
    }

    private void sendOffer(WaitlistEntry entry, SlotFreedEvent event) {
        Long offeredEmployeeId = entry.getEmployeeId() != null ? entry.getEmployeeId() : event.employeeId();
        entry.markNotified(offeredEmployeeId, event.scheduledAt(), event.endAt());
        waitlistEntryRepository.save(entry);

        PublicActionToken token = tokenService.issueWaitlistOfferToken(event.businessId(), entry.getId());
        Customer customer = customerService.findByIdOrThrow(entry.getCustomerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(entry.getServiceId());
        String offerLink = emailProperties.baseUrl() + "/waitlist-offer/" + token.getToken();

        notificationService.sendWaitlistSlotOffer(event.businessId(), customer, service, event.scheduledAt(), offerLink);
        log.info("Waitlist teklifi gönderildi: waitlistEntryId={}, businessId={}", entry.getId(), event.businessId());
    }
}
