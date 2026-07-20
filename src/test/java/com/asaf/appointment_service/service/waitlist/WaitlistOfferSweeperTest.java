package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.domain.event.SlotFreedEvent;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistOfferSweeperTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WaitlistOfferSweeper sweeper;

    @Test
    void should_expireStaleOfferAndRepublishSlotFreedEvent_when_offerOlderThanTwoHours() {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(1L).customerId(10L).serviceId(3L).build();
        entry.markNotified(7L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));
        ReflectionTestUtils.setField(entry, "id", 5L);

        when(waitlistEntryRepository.findByStatusAndNotifiedAtBefore(
                org.mockito.ArgumentMatchers.eq(WaitlistStatus.NOTIFIED), any(LocalDateTime.class)))
            .thenReturn(List.of(entry));

        sweeper.expireStaleOffers();

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        verify(waitlistEntryRepository).save(entry);

        ArgumentCaptor<SlotFreedEvent> captor = ArgumentCaptor.forClass(SlotFreedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().businessId()).isEqualTo(1L);
        assertThat(captor.getValue().employeeId()).isEqualTo(7L);
        assertThat(captor.getValue().serviceId()).isEqualTo(3L);
    }

    @Test
    void should_doNothing_when_noStaleOffers() {
        when(waitlistEntryRepository.findByStatusAndNotifiedAtBefore(
                org.mockito.ArgumentMatchers.eq(WaitlistStatus.NOTIFIED), any(LocalDateTime.class)))
            .thenReturn(List.of());

        sweeper.expireStaleOffers();

        verify(waitlistEntryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
