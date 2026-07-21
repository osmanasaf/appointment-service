package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import com.asaf.appointment_service.entity.calendar.EmployeeExternalBusy;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.dto.GoogleBusyBlock;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.repository.calendar.EmployeeExternalBusyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bağlı çalışanların kişisel Google Takvimi'ndeki etkinlikleri periyodik olarak çekip
 * dolu blok (EmployeeExternalBusy) olarak saklar (içe akış). Sistemin kendi yazdığı
 * etkinlikler (randevum_managed extended property) GoogleCalendarClient içinde filtrelenip
 * hariç tutulur — döngü koruması.
 */
@Service
public class GoogleBusyBlockSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoogleBusyBlockSyncScheduler.class);
    private static final int LOOKAHEAD_DAYS = 30;

    private final CalendarConnectionRepository connectionRepository;
    private final CalendarConnectionService connectionService;
    private final GoogleCalendarClient googleCalendarClient;
    private final EmployeeExternalBusyRepository busyRepository;

    public GoogleBusyBlockSyncScheduler(CalendarConnectionRepository connectionRepository,
                                         CalendarConnectionService connectionService,
                                         GoogleCalendarClient googleCalendarClient,
                                         EmployeeExternalBusyRepository busyRepository) {
        this.connectionRepository = connectionRepository;
        this.connectionService = connectionService;
        this.googleCalendarClient = googleCalendarClient;
        this.busyRepository = busyRepository;
    }

    @Scheduled(cron = "${google.calendar.sync.scheduler.cron:0 */5 * * * *}")
    @Transactional
    public void syncBusyBlocks() {
        List<CalendarConnection> connections = connectionRepository.findByStatus(CalendarConnectionStatus.CONNECTED);
        int syncedCount = 0;
        for (CalendarConnection connection : connections) {
            try {
                syncOne(connection);
                syncedCount++;
            } catch (Exception e) {
                log.error("Google Takvim dolu blok senkronu başarısız: employeeId={}, error={}",
                    connection.getEmployeeId(), e.getMessage());
            }
        }
        log.info("Google Takvim dolu blok senkronu tamamlandı: {} bağlantı işlendi", syncedCount);
    }

    private void syncOne(CalendarConnection connection) {
        String accessToken = connectionService.resolveAccessToken(connection);
        if (accessToken == null) {
            return;
        }
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(LOOKAHEAD_DAYS);
        List<GoogleBusyBlock> busyBlocks = googleCalendarClient.listBusyBlocks(
            accessToken, connection.getGoogleCalendarId(), from, to);

        busyRepository.deleteByEmployeeId(connection.getEmployeeId());
        for (GoogleBusyBlock block : busyBlocks) {
            busyRepository.save(EmployeeExternalBusy.builder()
                .businessId(connection.getBusinessId())
                .employeeId(connection.getEmployeeId())
                .externalEventId(block.externalEventId())
                .startAt(block.start())
                .endAt(block.end())
                .build());
        }

        connection.updateLastSyncedAt();
        connectionRepository.save(connection);
    }
}
