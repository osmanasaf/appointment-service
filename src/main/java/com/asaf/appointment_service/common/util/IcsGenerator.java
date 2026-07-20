package com.asaf.appointment_service.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bağımsız (framework/entity'den habersiz) RFC 5545 .ics üretici.
 * Randevu saatlerinin sistemde işletme yerel saatiyle (varsayılan Europe/Istanbul)
 * tutulduğu varsayılır; wire formatına UTC'ye çevrilerek yazılır — bu sayede alıcı
 * takvim uygulaması kendi saat dilimine göre doğru yerel saati gösterir ve ayrı bir
 * VTIMEZONE bloğuna gerek kalmaz.
 */
public final class IcsGenerator {

    private static final ZoneId DEFAULT_BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter ICS_UTC_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String LINE_BREAK = "\r\n";

    private IcsGenerator() {}

    public record IcsEvent(
        Long appointmentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String summary,
        String location,
        String description,
        boolean cancelled
    ) {}

    public static String generate(IcsEvent event) {
        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR").append(LINE_BREAK);
        ics.append("VERSION:2.0").append(LINE_BREAK);
        ics.append("PRODID:-//Randevum//Appointment//TR").append(LINE_BREAK);
        ics.append("CALSCALE:GREGORIAN").append(LINE_BREAK);
        ics.append("METHOD:").append(event.cancelled() ? "CANCEL" : "PUBLISH").append(LINE_BREAK);
        ics.append("BEGIN:VEVENT").append(LINE_BREAK);
        ics.append("UID:appointment-").append(event.appointmentId()).append("@randevum.pro").append(LINE_BREAK);
        ics.append("DTSTAMP:").append(toUtc(LocalDateTime.now())).append(LINE_BREAK);
        ics.append("DTSTART:").append(toUtc(event.startTime())).append(LINE_BREAK);
        ics.append("DTEND:").append(toUtc(event.endTime())).append(LINE_BREAK);
        ics.append("SUMMARY:").append(escape(event.summary())).append(LINE_BREAK);
        if (event.location() != null && !event.location().isBlank()) {
            ics.append("LOCATION:").append(escape(event.location())).append(LINE_BREAK);
        }
        if (event.description() != null && !event.description().isBlank()) {
            ics.append("DESCRIPTION:").append(escape(event.description())).append(LINE_BREAK);
        }
        ics.append("SEQUENCE:").append(event.cancelled() ? 1 : 0).append(LINE_BREAK);
        ics.append("STATUS:").append(event.cancelled() ? "CANCELLED" : "CONFIRMED").append(LINE_BREAK);
        ics.append("END:VEVENT").append(LINE_BREAK);
        ics.append("END:VCALENDAR").append(LINE_BREAK);
        return ics.toString();
    }

    private static String toUtc(LocalDateTime businessLocalTime) {
        ZonedDateTime utc = businessLocalTime.atZone(DEFAULT_BUSINESS_ZONE).withZoneSameInstant(ZoneOffset.UTC);
        return utc.format(ICS_UTC_FORMAT);
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n");
    }
}
