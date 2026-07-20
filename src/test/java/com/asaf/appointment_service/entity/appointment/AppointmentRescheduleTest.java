package com.asaf.appointment_service.entity.appointment;

import com.asaf.appointment_service.common.statemachine.StateMachine;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentRescheduleTest {

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        return Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(LocalDateTime.now().plusDays(1), 60)
            .status(status)
            .build();
    }

    @Test
    void should_confirmAndMoveSlot_when_rescheduleCalledWithAutoConfirmTrue() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
        LocalDateTime newStart = LocalDateTime.now().plusDays(2);
        LocalDateTime newEnd = newStart.plusMinutes(45);

        appointment.reschedule(5L, newStart, newEnd, 45, true);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getEmployeeId()).isEqualTo(5L);
        assertThat(appointment.getScheduledAt()).isEqualTo(newStart);
        assertThat(appointment.getEndAt()).isEqualTo(newEnd);
        assertThat(appointment.getDurationMinutes()).isEqualTo(45);
        assertThat(appointment.getConfirmedAt()).isNotNull();
        assertThat(appointment.isRisky()).isFalse();
    }

    @Test
    void should_movePending_when_rescheduleCalledWithAutoConfirmFalse() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
        LocalDateTime newStart = LocalDateTime.now().plusDays(2);

        appointment.reschedule(5L, newStart, newStart.plusMinutes(30), 30, false);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getConfirmedAt()).isNull();
    }

    @Test
    void should_throwInvalidStateTransition_when_rescheduleCalledFromCancelledStatus() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CANCELLED);
        LocalDateTime newStart = LocalDateTime.now().plusDays(2);

        assertThatThrownBy(() -> appointment.reschedule(5L, newStart, newStart.plusMinutes(30), 30, true))
            .isInstanceOf(StateMachine.InvalidStateTransitionException.class);
    }

    @Test
    void should_throwInvalidStateTransition_when_rescheduleCalledFromRiskyStatusWithoutAutoConfirm() {
        // RISKY -> PENDING geçişi tanımlı değil; reschedule() sadece RESCHEDULE_REQUESTED kaynaklı akış içindir
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RISKY);
        LocalDateTime newStart = LocalDateTime.now().plusDays(2);

        assertThatThrownBy(() -> appointment.reschedule(5L, newStart, newStart.plusMinutes(30), 30, false))
            .isInstanceOf(StateMachine.InvalidStateTransitionException.class);
    }
}
