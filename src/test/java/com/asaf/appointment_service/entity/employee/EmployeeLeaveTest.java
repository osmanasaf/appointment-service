package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.domain.valueobject.LeavePeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeLeaveTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 7);
    private static final LocalDate NEXT_WEEK = TODAY.plusWeeks(1);
    private static final Long EMPLOYEE_ID = 1L;

    private EmployeeLeave buildLeave(LeavePeriod period) {
        return EmployeeLeave.builder()
            .employeeId(EMPLOYEE_ID)
            .period(period)
            .leaveType(LeaveType.ANNUAL)
            .build();
    }

    @Test
    void should_create_active_leave_with_correct_fields() {
        LeavePeriod period = LeavePeriod.fullDay(TODAY, NEXT_WEEK);
        EmployeeLeave leave = buildLeave(period);

        assertThat(leave.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(leave.getStartDate()).isEqualTo(TODAY);
        assertThat(leave.getEndDate()).isEqualTo(NEXT_WEEK);
        assertThat(leave.getLeaveType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.ACTIVE);
        assertThat(leave.isActive()).isTrue();
        assertThat(leave.isFullDay()).isTrue();
    }

    @Test
    void should_cancel_active_leave() {
        EmployeeLeave leave = buildLeave(LeavePeriod.fullDay(TODAY, NEXT_WEEK));

        leave.cancel();

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(leave.isActive()).isFalse();
    }

    @Test
    void should_throw_when_cancelling_already_cancelled_leave() {
        EmployeeLeave leave = buildLeave(LeavePeriod.fullDay(TODAY, NEXT_WEEK));
        leave.cancel();

        assertThatThrownBy(leave::cancel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("zaten iptal edilmiş");
    }

    @Test
    void should_detect_overlap_with_date_in_range() {
        EmployeeLeave leave = buildLeave(LeavePeriod.fullDay(TODAY, NEXT_WEEK));

        assertThat(leave.overlapsWithDate(TODAY)).isTrue();
        assertThat(leave.overlapsWithDate(TODAY.plusDays(3))).isTrue();
        assertThat(leave.overlapsWithDate(NEXT_WEEK.plusDays(1))).isFalse();
    }

    @Test
    void should_overlap_with_datetime_for_full_day() {
        EmployeeLeave leave = buildLeave(LeavePeriod.fullDay(TODAY, TODAY));
        LocalDateTime midday = LocalDateTime.of(TODAY, LocalTime.NOON);
        LocalDateTime tomorrow = LocalDateTime.of(TODAY.plusDays(1), LocalTime.NOON);

        assertThat(leave.overlapsWithDateTime(midday)).isTrue();
        assertThat(leave.overlapsWithDateTime(tomorrow)).isFalse();
    }

    @Test
    void should_overlap_with_datetime_for_partial_day() {
        LeavePeriod period = LeavePeriod.partialDay(TODAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        EmployeeLeave leave = buildLeave(period);

        LocalDateTime inside = LocalDateTime.of(TODAY, LocalTime.of(11, 0));
        LocalDateTime before = LocalDateTime.of(TODAY, LocalTime.of(8, 0));
        LocalDateTime after = LocalDateTime.of(TODAY, LocalTime.of(14, 0));

        assertThat(leave.overlapsWithDateTime(inside)).isTrue();
        assertThat(leave.overlapsWithDateTime(before)).isFalse();
        assertThat(leave.overlapsWithDateTime(after)).isFalse();
    }

    @Test
    void should_throw_when_employee_id_missing() {
        assertThatThrownBy(() ->
            EmployeeLeave.builder()
                .period(LeavePeriod.fullDay(TODAY, NEXT_WEEK))
                .leaveType(LeaveType.SICK)
                .build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_leave_type_missing() {
        assertThatThrownBy(() ->
            EmployeeLeave.builder()
                .employeeId(EMPLOYEE_ID)
                .period(LeavePeriod.fullDay(TODAY, NEXT_WEEK))
                .build())
            .isInstanceOf(NullPointerException.class);
    }
}
