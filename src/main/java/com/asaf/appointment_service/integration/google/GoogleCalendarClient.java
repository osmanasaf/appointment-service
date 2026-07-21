package com.asaf.appointment_service.integration.google;

import com.asaf.appointment_service.integration.google.dto.GoogleBusyBlock;
import com.asaf.appointment_service.integration.google.dto.GoogleCalendarEventData;
import com.asaf.appointment_service.integration.google.dto.GoogleTokenResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface GoogleCalendarClient {

    String buildAuthorizationUrl(String state, String redirectUri);

    GoogleTokenResponse exchangeAuthorizationCode(String code, String redirectUri);

    GoogleTokenResponse refreshAccessToken(String refreshToken);

    /**
     * externalEventId null/boş ise yeni etkinlik oluşturur, doluysa günceller.
     * Her iki durumda da Google'daki etkinlik id'sini döner.
     */
    String upsertEvent(String accessToken, String calendarId, String externalEventId, GoogleCalendarEventData event);

    void deleteEvent(String accessToken, String calendarId, String externalEventId);

    /**
     * Belirtilen aralıktaki etkinlikleri döner; sistemin kendi yazdığı (randevum_managed
     * extended property'li) etkinlikler döngü koruması için filtrelenip hariç tutulur.
     */
    List<GoogleBusyBlock> listBusyBlocks(String accessToken, String calendarId, LocalDateTime from, LocalDateTime to);
}
