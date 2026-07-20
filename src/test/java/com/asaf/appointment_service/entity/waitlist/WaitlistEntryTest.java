package com.asaf.appointment_service.entity.waitlist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaitlistEntryTest {

    private WaitlistEntry entryWithPreference(Long employeeId, LocalDate from, LocalDate until) {
        return WaitlistEntry.builder()
            .businessId(1L).customerId(10L).serviceId(3L)
            .employeeId(employeeId)
            .preferredFrom(from).preferredUntil(until)
            .build();
    }

    @Test
    void should_match_when_serviceMatchesAndNoEmployeeOrDatePreference() {
        WaitlistEntry entry = entryWithPreference(null, null, null);

        boolean matches = entry.matchesFreedSlot(3L, 5L, LocalDateTime.now().plusDays(10));

        assertThat(matches).isTrue();
    }

    @Test
    void should_notMatch_when_serviceDiffers() {
        WaitlistEntry entry = entryWithPreference(null, null, null);

        assertThat(entry.matchesFreedSlot(99L, 5L, LocalDateTime.now())).isFalse();
    }

    @Test
    void should_notMatch_when_employeePreferenceSetAndDiffers() {
        WaitlistEntry entry = entryWithPreference(5L, null, null);

        assertThat(entry.matchesFreedSlot(3L, 6L, LocalDateTime.now())).isFalse();
    }

    @Test
    void should_match_when_employeePreferenceSetAndEquals() {
        WaitlistEntry entry = entryWithPreference(5L, null, null);

        assertThat(entry.matchesFreedSlot(3L, 5L, LocalDateTime.now())).isTrue();
    }

    @Test
    void should_notMatch_when_dateBeforePreferredWindow() {
        LocalDate from = LocalDate.now().plusDays(5);
        WaitlistEntry entry = entryWithPreference(null, from, null);

        assertThat(entry.matchesFreedSlot(3L, 5L, LocalDateTime.now().plusDays(1))).isFalse();
    }

    @Test
    void should_notMatch_when_dateAfterPreferredWindow() {
        LocalDate until = LocalDate.now().plusDays(5);
        WaitlistEntry entry = entryWithPreference(null, null, until);

        assertThat(entry.matchesFreedSlot(3L, 5L, LocalDateTime.now().plusDays(10))).isFalse();
    }

    @Test
    void should_notMatch_when_statusNotWaiting() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        entry.markNotified(5L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));

        assertThat(entry.matchesFreedSlot(3L, 5L, LocalDateTime.now().plusDays(1))).isFalse();
    }

    @Test
    void should_transitionToNotified_when_markNotifiedCalledFromWaiting() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        LocalDateTime offeredAt = LocalDateTime.now().plusDays(2);
        LocalDateTime offeredEnd = offeredAt.plusMinutes(45);

        entry.markNotified(7L, offeredAt, offeredEnd);

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.NOTIFIED);
        assertThat(entry.getNotifiedAt()).isNotNull();
        assertThat(entry.getOfferedEmployeeId()).isEqualTo(7L);
        assertThat(entry.getOfferedScheduledAt()).isEqualTo(offeredAt);
        assertThat(entry.getOfferedEndAt()).isEqualTo(offeredEnd);
    }

    @Test
    void should_throwIllegalState_when_markNotifiedCalledTwice() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        entry.markNotified(7L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));

        assertThatThrownBy(() -> entry.markNotified(8L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_transitionToExpired_when_markExpiredCalledFromNotified() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        entry.markNotified(7L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));

        entry.markExpired();

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
    }

    @Test
    void should_throwIllegalState_when_markExpiredCalledFromWaiting() {
        WaitlistEntry entry = entryWithPreference(null, null, null);

        assertThatThrownBy(entry::markExpired).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_transitionToConverted_when_markConvertedCalledFromNotified() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        entry.markNotified(7L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));

        entry.markConverted();

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.CONVERTED);
    }

    @Test
    void should_cancel_when_statusIsWaitingOrNotified() {
        WaitlistEntry waiting = entryWithPreference(null, null, null);
        waiting.cancel();
        assertThat(waiting.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);

        WaitlistEntry notified = entryWithPreference(null, null, null);
        notified.markNotified(7L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        notified.cancel();
        assertThat(notified.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
    }

    @Test
    void should_throwIllegalState_when_cancelCalledFromConverted() {
        WaitlistEntry entry = entryWithPreference(null, null, null);
        entry.markNotified(7L, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        entry.markConverted();

        assertThatThrownBy(entry::cancel).isInstanceOf(IllegalStateException.class);
    }
}
