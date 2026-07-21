package com.asaf.appointment_service.integration.google;

import com.asaf.appointment_service.integration.google.dto.GoogleBusyBlock;
import com.asaf.appointment_service.integration.google.dto.GoogleCalendarEventData;
import com.asaf.appointment_service.integration.google.dto.GoogleTokenResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockGoogleCalendarClient implements GoogleCalendarClient {

    private static final Logger log = LoggerFactory.getLogger(MockGoogleCalendarClient.class);

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        return "https://accounts.google.com/o/oauth2/v2/auth?mock=true&state=" + state;
    }

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(String code, String redirectUri) {
        log.info("[MOCK] Google OAuth kod değişimi: code={}", code);
        return GoogleTokenResponse.success("mock-access-token", "mock-refresh-token", 3600);
    }

    @Override
    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        log.info("[MOCK] Google access token yenilendi");
        return GoogleTokenResponse.success("mock-access-token", null, 3600);
    }

    @Override
    public String upsertEvent(String accessToken, String calendarId, String externalEventId,
                               GoogleCalendarEventData event) {
        String eventId = externalEventId != null ? externalEventId : "mock-event-" + UUID.randomUUID();
        log.info("[MOCK] Google Calendar etkinliği yazıldı: calendarId={}, eventId={}, summary={}",
            calendarId, eventId, event.summary());
        return eventId;
    }

    @Override
    public void deleteEvent(String accessToken, String calendarId, String externalEventId) {
        log.info("[MOCK] Google Calendar etkinliği silindi: calendarId={}, eventId={}", calendarId, externalEventId);
    }

    @Override
    public List<GoogleBusyBlock> listBusyBlocks(String accessToken, String calendarId,
                                                 LocalDateTime from, LocalDateTime to) {
        return List.of();
    }
}
