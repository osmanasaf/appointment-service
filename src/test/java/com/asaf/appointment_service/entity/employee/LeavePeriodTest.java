package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.domain.valueobject.LeavePeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeavePeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 7);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate NEXT_WEEK = TODAY.plusWeeks(1);

    @Test
    void should_create_full_day_leave_when_no_times_provided() {
        LeavePeriod period = LeavePeriod.fullDay(TODAY, NEXT_WEEK);

        assertThat(period.isFullDay()).isTrue();
        assertThat(period.startDate()).isEqualTo(TODAY);
        assertThat(period.endDate()).isEqualTo(NEXT_WEEK);
    }

    @Test
    void should_create_partial_day_leave_when_times_provided() {
        LeavePeriod period = LeavePeriod.partialDay(TODAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

        assertThat(period.isFullDay()).isFalse();
        assertThat(period.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(period.endTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void should_throw_when_end_date_before_start_date() {
        assertThatThrownBy(() -> LeavePeriod.fullDay(NEXT_WEEK, TODAY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bitiş tarihi başlangıç tarihinden önce olamaz");
    }

    @Test
    void should_throw_when_only_start_time_provided() {
        assertThatThrownBy(() -> new LeavePeriod(TODAY, TODAY, LocalTime.of(9, 0), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("birlikte belirtilmelidir");
    }

    @Test
    void should_throw_when_start_time_after_end_time() {
        assertThatThrownBy(() ->
            LeavePeriod.partialDay(TODAY, LocalTime.of(14, 0), LocalTime.of(9, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("başlangıç saati bitiş saatinden önce olmalıdır");
    }

    @Test
    void should_overlap_with_date_within_range() {
        LeavePeriod period = LeavePeriod.fullDay(TODAY, NEXT_WEEK);

        assertThat(period.overlapsWithDate(TODAY)).isTrue();
        assertThat(period.overlapsWithDate(TOMORROW)).isTrue();
        assertThat(period.overlapsWithDate(NEXT_WEEK)).isTrue();
        assertThat(period.overlapsWithDate(NEXT_WEEK.plusDays(1))).isFalse();
        assertThat(period.overlapsWithDate(TODAY.minusDays(1))).isFalse();
    }

    @Test
    void should_overlap_with_datetime_for_full_day_leave() {
        LeavePeriod period = LeavePeriod.fullDay(TODAY, TODAY);
        LocalDateTime morningSlot = LocalDateTime.of(TODAY, LocalTime.of(9, 0));
        LocalDateTime eveningSlot = LocalDateTime.of(TODAY, LocalTime.of(20, 0));
        LocalDateTime nextDaySlot = LocalDateTime.of(TOMORROW, LocalTime.of(9, 0));

        assertThat(period.overlapsWithDateTime(morningSlot)).isTrue();
        assertThat(period.overlapsWithDateTime(eveningSlot)).isTrue();
        assertThat(period.overlapsWithDateTime(nextDaySlot)).isFalse();
    }

    @Test
    void should_overlap_with_datetime_for_partial_day_leave() {
        LeavePeriod period = LeavePeriod.partialDay(TODAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        LocalDateTime insideSlot = LocalDateTime.of(TODAY, LocalTime.of(10, 0));
        LocalDateTime beforeSlot = LocalDateTime.of(TODAY, LocalTime.of(8, 0));
        LocalDateTime afterSlot = LocalDateTime.of(TODAY, LocalTime.of(14, 0));

        assertThat(period.overlapsWithDateTime(insideSlot)).isTrue();
        assertThat(period.overlapsWithDateTime(beforeSlot)).isFalse();
        assertThat(period.overlapsWithDateTime(afterSlot)).isFalse();
    }

    @Test
    void should_overlap_with_time_slot_when_ranges_intersect() {
        LeavePeriod period = LeavePeriod.partialDay(TODAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

        assertThat(period.overlapsWithTimeSlot(TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();
        assertThat(period.overlapsWithTimeSlot(TODAY, LocalTime.of(8, 0), LocalTime.of(10, 0))).isTrue();
        assertThat(period.overlapsWithTimeSlot(TODAY, LocalTime.of(12, 0), LocalTime.of(14, 0))).isTrue();
        assertThat(period.overlapsWithTimeSlot(TODAY, LocalTime.of(13, 0), LocalTime.of(15, 0))).isFalse();
        assertThat(period.overlapsWithTimeSlot(TODAY, LocalTime.of(7, 0), LocalTime.of(9, 0))).isFalse();
    }
}
