package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.dto.GoogleBusyBlock;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.repository.calendar.EmployeeExternalBusyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleBusyBlockSyncSchedulerTest {

    private static final Long EMPLOYEE_ID = 2L;

    @Mock
    private CalendarConnectionRepository connectionRepository;
    @Mock
    private CalendarConnectionService connectionService;
    @Mock
    private GoogleCalendarClient googleCalendarClient;
    @Mock
    private EmployeeExternalBusyRepository busyRepository;

    private GoogleBusyBlockSyncScheduler scheduler;

    private void setUp() {
        scheduler = new GoogleBusyBlockSyncScheduler(connectionRepository, connectionService, googleCalendarClient, busyRepository);
    }

    private CalendarConnection connection() {
        return CalendarConnection.builder()
            .businessId(1L).employeeId(EMPLOYEE_ID).refreshTokenEncrypted("enc").build();
    }

    @Test
    void should_replaceBusyBlocks_when_connectionActiveAndTokenValid() {
        setUp();
        CalendarConnection connection = connection();
        when(connectionRepository.findByStatus(CalendarConnectionStatus.CONNECTED)).thenReturn(List.of(connection));
        when(connectionService.resolveAccessToken(connection)).thenReturn("access-token");
        GoogleBusyBlock block = new GoogleBusyBlock("ext-1", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3));
        when(googleCalendarClient.listBusyBlocks(eq("access-token"), eq(connection.getGoogleCalendarId()), any(), any()))
            .thenReturn(List.of(block));

        scheduler.syncBusyBlocks();

        verify(busyRepository).deleteByEmployeeId(EMPLOYEE_ID);
        verify(busyRepository, times(1)).save(any());
        verify(connectionRepository).save(connection);
    }

    @Test
    void should_skipConnection_when_tokenResolutionFails() {
        setUp();
        CalendarConnection connection = connection();
        when(connectionRepository.findByStatus(CalendarConnectionStatus.CONNECTED)).thenReturn(List.of(connection));
        when(connectionService.resolveAccessToken(connection)).thenReturn(null);

        scheduler.syncBusyBlocks();

        verify(busyRepository, never()).deleteByEmployeeId(anyLong());
        verify(googleCalendarClient, never()).listBusyBlocks(any(), any(), any(), any());
    }

    @Test
    void should_continueOtherConnections_when_oneThrows() {
        setUp();
        CalendarConnection failing = connection();
        CalendarConnection healthy = CalendarConnection.builder()
            .businessId(1L).employeeId(9L).refreshTokenEncrypted("enc2").build();
        when(connectionRepository.findByStatus(CalendarConnectionStatus.CONNECTED))
            .thenReturn(List.of(failing, healthy));
        when(connectionService.resolveAccessToken(failing)).thenThrow(new RuntimeException("boom"));
        when(connectionService.resolveAccessToken(healthy)).thenReturn("access-token");
        when(googleCalendarClient.listBusyBlocks(eq("access-token"), any(), any(), any())).thenReturn(List.of());

        scheduler.syncBusyBlocks();

        verify(busyRepository).deleteByEmployeeId(9L);
        verify(busyRepository, never()).deleteByEmployeeId(EMPLOYEE_ID);
    }
}
