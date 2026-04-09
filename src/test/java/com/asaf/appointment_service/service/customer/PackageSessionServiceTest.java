package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.customer.PackageSession;
import com.asaf.appointment_service.entity.customer.PackageSessionStatus;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.PackageSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageSessionServiceTest {

    @Mock private PackageSessionRepository sessionRepository;
    @Mock private CustomerPackageRepository packageRepository;

    @InjectMocks
    private PackageSessionService sessionService;

    @Test
    void createSessionsForPackage_shouldSaveAllAtOnce() {
        CustomerPackage pkg = buildPackage(3);

        sessionService.createSessionsForPackage(pkg);

        verify(sessionRepository).saveAll(anyList());
    }

    @Test
    void assignAppointment_whenPendingSession_shouldAssignAndSave() {
        PackageSession session = buildSession(PackageSessionStatus.PENDING);
        when(sessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PackageSession result = sessionService.assignAppointment(1L, 10L, LocalDateTime.now().plusDays(1));

        assertThat(result.getStatus()).isEqualTo(PackageSessionStatus.SCHEDULED);
        assertThat(result.getAppointmentId()).isEqualTo(10L);
    }

    @Test
    void assignAppointment_whenAlreadyScheduled_shouldThrowIllegalStateException() {
        PackageSession session = buildSession(PackageSessionStatus.SCHEDULED);
        when(sessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.assignAppointment(1L, 99L, LocalDateTime.now()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unassignAppointment_whenScheduled_shouldReturnToPendingStatus() {
        PackageSession session = buildSession(PackageSessionStatus.SCHEDULED);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PackageSession result = sessionService.unassignAppointment(1L);

        assertThat(result.getStatus()).isEqualTo(PackageSessionStatus.PENDING);
        assertThat(result.getAppointmentId()).isNull();
    }

    @Test
    void unassignAppointment_whenNotScheduled_shouldThrowIllegalStateException() {
        PackageSession session = buildSession(PackageSessionStatus.PENDING);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.unassignAppointment(1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markNoShow_shouldBurnSessionAndDeductFromPackage() {
        PackageSession session = buildSession(PackageSessionStatus.SCHEDULED);
        CustomerPackage pkg = buildPackage(3);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.findById(session.getCustomerPackageId())).thenReturn(Optional.of(pkg));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sessionService.markNoShow(1L);

        assertThat(session.getStatus()).isEqualTo(PackageSessionStatus.NO_SHOW);
        verify(packageRepository).save(pkg);
    }

    @Test
    void assignAppointment_raceCondition_whenConcurrentAssignAttempt_shouldThrowForSecondCaller() {
        PackageSession session = buildSession(PackageSessionStatus.PENDING);

        when(sessionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            PackageSession saved = inv.getArgument(0);
            return saved;
        });

        sessionService.assignAppointment(1L, 10L, LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> sessionService.assignAppointment(1L, 20L, LocalDateTime.now().plusDays(1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Sadece bekleyen seansa");
    }

    private PackageSession buildSession(PackageSessionStatus status) {
        return PackageSession.builder()
            .id(1L).customerPackageId(5L).sessionNumber(1).status(status).build();
    }

    private CustomerPackage buildPackage(int remaining) {
        return CustomerPackage.builder()
            .id(5L).customerId(1L).businessId(10L)
            .active(true).totalSessions(remaining + 2).remainingSessions(remaining).build();
    }
}
