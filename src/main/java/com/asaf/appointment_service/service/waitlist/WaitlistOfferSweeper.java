package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.domain.event.SlotFreedEvent;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yanıtsız kalan waitlist tekliflerini süresi (2 saat) dolunca EXPIRED yapar
 * ve aynı boşalan slotu tekrar teklif akışına sokarak sıradaki adaya geçirir.
 */
@Service
public class WaitlistOfferSweeper {

    private static final Logger log = LoggerFactory.getLogger(WaitlistOfferSweeper.class);
    private static final int OFFER_VALID_HOURS = 2;

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WaitlistOfferSweeper(WaitlistEntryRepository waitlistEntryRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${waitlist.offer.sweeper.interval:900000}")
    @Transactional
    public void expireStaleOffers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(OFFER_VALID_HOURS);
        List<WaitlistEntry> staleOffers = waitlistEntryRepository
            .findByStatusAndNotifiedAtBefore(WaitlistStatus.NOTIFIED, cutoff);

        if (staleOffers.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (WaitlistEntry entry : staleOffers) {
            try {
                expireAndRecascade(entry);
                expiredCount++;
            } catch (Exception e) {
                log.error("Waitlist teklifi süresi doldurulamadı: waitlistEntryId={}, error={}",
                    entry.getId(), e.getMessage());
            }
        }
        log.info("Waitlist offer sweeper tamamlandı: {} teklif süresi doldu", expiredCount);
    }

    private void expireAndRecascade(WaitlistEntry entry) {
        Long businessId = entry.getBusinessId();
        Long employeeId = entry.getOfferedEmployeeId();
        Long serviceId = entry.getServiceId();
        LocalDateTime scheduledAt = entry.getOfferedScheduledAt();
        LocalDateTime endAt = entry.getOfferedEndAt();

        entry.markExpired();
        waitlistEntryRepository.save(entry);

        eventPublisher.publishEvent(new SlotFreedEvent(businessId, employeeId, serviceId, scheduledAt, endAt));
    }
}
