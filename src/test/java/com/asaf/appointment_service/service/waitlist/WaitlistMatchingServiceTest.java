package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.domain.event.SlotFreedEvent;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistMatchingServiceTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;
    @Mock
    private PublicActionTokenService tokenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationSettingsService notificationSettingsService;
    @Mock
    private CustomerService customerService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private WaitlistMatchingService matchingService;

    private WaitlistEntry entryAt(Long id, LocalDateTime createdAt) {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(1L).customerId(10L).serviceId(3L).build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", createdAt);
        return entry;
    }

    @Test
    void should_notifyOldestWaitingCandidate_when_slotFreed() {
        WaitlistEntry older = entryAt(1L, LocalDateTime.now().minusDays(2));
        WaitlistEntry newer = entryAt(2L, LocalDateTime.now().minusDays(1));
        SlotFreedEvent event = new SlotFreedEvent(1L, 7L, 3L,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));

        when(waitlistEntryRepository.findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(1L, 3L, WaitlistStatus.WAITING))
            .thenReturn(List.of(older, newer));
        when(notificationSettingsService.getRemainingWhatsappQuota(1L)).thenReturn(10);
        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(older));
        when(tokenService.issueWaitlistOfferToken(1L, 1L)).thenReturn(offerToken(1L));
        when(customerService.findByIdOrThrow(10L)).thenReturn(
            Customer.builder().businessId(1L).name("Ali").phone("+90", "5551234567").build());
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(
            ServiceDefinition.builder().businessId(1L).name("Saç Kesimi").durationMinutes(30).buffers(0, 0).build());
        when(emailProperties.baseUrl()).thenReturn("https://randevum.pro");

        matchingService.onSlotFreed(event);

        assertThat(older.getStatus()).isEqualTo(WaitlistStatus.NOTIFIED);
        assertThat(newer.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        verify(notificationService).sendWaitlistSlotOffer(eq(1L), any(), any(), any(), contains("randevum.pro"));
    }

    @Test
    void should_doNothing_when_noWaitingCandidateMatches() {
        SlotFreedEvent event = new SlotFreedEvent(1L, 7L, 3L,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));
        when(waitlistEntryRepository.findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(1L, 3L, WaitlistStatus.WAITING))
            .thenReturn(List.of());

        matchingService.onSlotFreed(event);

        verify(notificationService, never()).sendWaitlistSlotOffer(any(), any(), any(), any(), any());
        verify(tokenService, never()).issueWaitlistOfferToken(any(), any());
    }

    @Test
    void should_skipOffer_when_lockedEntryNoLongerWaiting() {
        // Aday listesi sorgusu WAITING görse de, kilitli yeniden-okuma sırasında başka bir
        // eşzamanlı SlotFreedEvent zaten bu kaydı NOTIFIED'a taşımış olabilir.
        WaitlistEntry candidate = entryAt(1L, LocalDateTime.now().minusDays(1));
        WaitlistEntry alreadyClaimed = entryAt(1L, LocalDateTime.now().minusDays(1));
        alreadyClaimed.markNotified(9L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusMinutes(30));
        SlotFreedEvent event = new SlotFreedEvent(1L, 7L, 3L,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));

        when(waitlistEntryRepository.findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(1L, 3L, WaitlistStatus.WAITING))
            .thenReturn(List.of(candidate));
        when(notificationSettingsService.getRemainingWhatsappQuota(1L)).thenReturn(10);
        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(alreadyClaimed));

        matchingService.onSlotFreed(event);

        verify(tokenService, never()).issueWaitlistOfferToken(any(), any());
        verify(notificationService, never()).sendWaitlistSlotOffer(any(), any(), any(), any(), any());
    }

    @Test
    void should_skipOffer_when_whatsappQuotaExhausted() {
        WaitlistEntry entry = entryAt(1L, LocalDateTime.now().minusDays(1));
        SlotFreedEvent event = new SlotFreedEvent(1L, 7L, 3L,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));

        when(waitlistEntryRepository.findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(1L, 3L, WaitlistStatus.WAITING))
            .thenReturn(List.of(entry));
        when(notificationSettingsService.getRemainingWhatsappQuota(1L)).thenReturn(0);

        matchingService.onSlotFreed(event);

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        verify(tokenService, never()).issueWaitlistOfferToken(any(), any());
        verify(notificationService, never()).sendWaitlistSlotOffer(any(), any(), any(), any(), any());
    }

    private PublicActionToken offerToken(Long subjectId) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(subjectId).type(PublicActionTokenType.WAITLIST_OFFER)
            .token("offer-tok").expiresAt(Instant.now().plusSeconds(7200)).build();
    }
}
