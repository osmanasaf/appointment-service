package com.asaf.appointment_service.integration.google;

import com.asaf.appointment_service.integration.google.config.GoogleCalendarProperties;
import com.asaf.appointment_service.integration.google.dto.GoogleBusyBlock;
import com.asaf.appointment_service.integration.google.dto.GoogleCalendarEventData;
import com.asaf.appointment_service.integration.google.dto.GoogleTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Google OAuth2 + Calendar API v3 REST çağrıları. Bağımlılık olarak yalnızca JDK'nın
 * kendi HttpClient'ı ve Jackson kullanılır (PayTRHttpClient ile aynı desen).
 */
public class GoogleCalendarHttpClient implements GoogleCalendarClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarHttpClient.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3/calendars";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String MANAGED_PROPERTY_KEY = "randevum_managed";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");

    private final GoogleCalendarProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleCalendarHttpClient(GoogleCalendarProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        String query = "client_id=" + encode(properties.clientId())
            + "&redirect_uri=" + encode(redirectUri)
            + "&response_type=code"
            + "&scope=" + encode(SCOPE)
            + "&access_type=offline"
            + "&prompt=consent"
            + "&state=" + encode(state);
        return AUTH_URL + "?" + query;
    }

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", properties.clientId());
        form.put("client_secret", properties.clientSecret());
        form.put("code", code);
        form.put("grant_type", "authorization_code");
        form.put("redirect_uri", redirectUri);
        return requestToken(form);
    }

    @Override
    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", properties.clientId());
        form.put("client_secret", properties.clientSecret());
        form.put("refresh_token", refreshToken);
        form.put("grant_type", "refresh_token");
        return requestToken(form);
    }

    @Override
    public String upsertEvent(String accessToken, String calendarId, String externalEventId,
                               GoogleCalendarEventData event) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("summary", event.summary());
        if (event.description() != null) {
            body.put("description", event.description());
        }
        body.set("start", dateTimeNode(event.start(), event.timeZone()));
        body.set("end", dateTimeNode(event.end(), event.timeZone()));
        body.putObject("extendedProperties").putObject("private").put(MANAGED_PROPERTY_KEY, "true");

        boolean isUpdate = externalEventId != null && !externalEventId.isBlank();
        String path = "/" + encode(calendarId) + "/events" + (isUpdate ? "/" + encode(externalEventId) : "");
        String method = isUpdate ? "PUT" : "POST";

        try {
            HttpResponse<String> response = sendJson(method, path, accessToken, body.toString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return json.path("id").asText(externalEventId);
            }
            throw new GoogleCalendarApiException(
                "Google Calendar etkinliği yazılamadı: status=%d, body=%s".formatted(response.statusCode(), response.body()));
        } catch (GoogleCalendarApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleCalendarApiException("Google Calendar isteği başarısız", e);
        }
    }

    @Override
    public void deleteEvent(String accessToken, String calendarId, String externalEventId) {
        String path = "/" + encode(calendarId) + "/events/" + encode(externalEventId);
        try {
            HttpResponse<String> response = sendJson("DELETE", path, accessToken, null);
            int status = response.statusCode();
            if (status >= 200 && status < 300 || status == 404 || status == 410) {
                return;
            }
            throw new GoogleCalendarApiException(
                "Google Calendar etkinliği silinemedi: status=%d, body=%s".formatted(status, response.body()));
        } catch (GoogleCalendarApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleCalendarApiException("Google Calendar silme isteği başarısız", e);
        }
    }

    @Override
    public List<GoogleBusyBlock> listBusyBlocks(String accessToken, String calendarId,
                                                 LocalDateTime from, LocalDateTime to) {
        String query = "timeMin=" + encode(toRfc3339(from))
            + "&timeMax=" + encode(toRfc3339(to))
            + "&singleEvents=true&maxResults=250&orderBy=startTime";
        String path = "/" + encode(calendarId) + "/events?" + query;

        try {
            HttpResponse<String> response = sendJson("GET", path, accessToken, null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GoogleCalendarApiException(
                    "Google Calendar etkinlikleri listelenemedi: status=%d".formatted(response.statusCode()));
            }
            JsonNode items = objectMapper.readTree(response.body()).path("items");
            List<GoogleBusyBlock> busyBlocks = new ArrayList<>();
            for (JsonNode item : items) {
                if (isSelfManaged(item) || !item.path("start").has("dateTime")) {
                    continue;
                }
                String eventId = item.path("id").asText();
                LocalDateTime start = parseRfc3339(item.path("start").path("dateTime").asText());
                LocalDateTime end = parseRfc3339(item.path("end").path("dateTime").asText());
                busyBlocks.add(new GoogleBusyBlock(eventId, start, end));
            }
            return busyBlocks;
        } catch (GoogleCalendarApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleCalendarApiException("Google Calendar listeleme isteği başarısız", e);
        }
    }

    private boolean isSelfManaged(JsonNode item) {
        return "true".equals(item.path("extendedProperties").path("private").path(MANAGED_PROPERTY_KEY).asText(null));
    }

    private GoogleTokenResponse requestToken(Map<String, String> form) {
        try {
            String body = form.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String accessToken = json.path("access_token").asText();
                String refreshToken = json.path("refresh_token").asText(null);
                long expiresIn = json.path("expires_in").asLong(3600);
                return GoogleTokenResponse.success(accessToken, refreshToken, expiresIn);
            }
            String error = json.path("error_description").asText(json.path("error").asText("unknown_error"));
            log.warn("Google OAuth token isteği başarısız: status={}, error={}", response.statusCode(), error);
            return GoogleTokenResponse.failure(error);
        } catch (Exception e) {
            log.error("Google OAuth token isteği sırasında hata", e);
            return GoogleTokenResponse.failure("Google ile iletişim kurulamadı");
        }
    }

    private HttpResponse<String> sendJson(String method, String path, String accessToken, String jsonBody)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(CALENDAR_API_BASE + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = jsonBody != null
            ? HttpRequest.BodyPublishers.ofString(jsonBody)
            : HttpRequest.BodyPublishers.noBody();
        builder.method(method, publisher);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private ObjectNode dateTimeNode(LocalDateTime dateTime, String timeZone) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("dateTime", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        node.put("timeZone", timeZone != null ? timeZone : BUSINESS_ZONE.getId());
        return node;
    }

    private String toRfc3339(LocalDateTime businessLocalTime) {
        return businessLocalTime.atZone(BUSINESS_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private LocalDateTime parseRfc3339(String value) {
        return OffsetDateTime.parse(value).atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class GoogleCalendarApiException extends RuntimeException {
        public GoogleCalendarApiException(String message) {
            super(message);
        }

        public GoogleCalendarApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
