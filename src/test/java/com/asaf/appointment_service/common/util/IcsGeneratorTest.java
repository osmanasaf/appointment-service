package com.asaf.appointment_service.common.util;

import com.asaf.appointment_service.common.util.IcsGenerator.IcsEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IcsGeneratorTest {

    private IcsEvent confirmedEvent() {
        return new IcsEvent(
            100L,
            LocalDateTime.of(2026, 7, 22, 13, 0),
            LocalDateTime.of(2026, 7, 22, 14, 0),
            "Saç Kesimi - Kuaför X",
            "İstiklal Cad. No:1",
            "Not: müşteri geç kalabilir",
            false
        );
    }

    @Test
    void should_useStableUid_when_generated() {
        String ics = IcsGenerator.generate(confirmedEvent());

        assertThat(ics).contains("UID:appointment-100@randevum.pro");
    }

    @Test
    void should_convertBusinessLocalTimeToUtc_when_generated() {
        String ics = IcsGenerator.generate(confirmedEvent());

        // Europe/Istanbul sabit UTC+3 (2016'dan beri DST yok): 13:00 yerel -> 10:00 UTC
        assertThat(ics).contains("DTSTART:20260722T100000Z");
        assertThat(ics).contains("DTEND:20260722T110000Z");
    }

    @Test
    void should_usePublishMethodAndConfirmedStatus_when_notCancelled() {
        String ics = IcsGenerator.generate(confirmedEvent());

        assertThat(ics).contains("METHOD:PUBLISH");
        assertThat(ics).contains("STATUS:CONFIRMED");
        assertThat(ics).contains("SEQUENCE:0");
    }

    @Test
    void should_useCancelMethodAndCancelledStatus_when_cancelled() {
        IcsEvent event = new IcsEvent(100L, LocalDateTime.of(2026, 7, 22, 13, 0),
            LocalDateTime.of(2026, 7, 22, 14, 0), "Saç Kesimi", null, null, true);

        String ics = IcsGenerator.generate(event);

        assertThat(ics).contains("METHOD:CANCEL");
        assertThat(ics).contains("STATUS:CANCELLED");
        assertThat(ics).contains("SEQUENCE:1");
    }

    @Test
    void should_omitLocationAndDescription_when_null() {
        IcsEvent event = new IcsEvent(100L, LocalDateTime.of(2026, 7, 22, 13, 0),
            LocalDateTime.of(2026, 7, 22, 14, 0), "Saç Kesimi", null, null, false);

        String ics = IcsGenerator.generate(event);

        assertThat(ics).doesNotContain("LOCATION:");
        assertThat(ics).doesNotContain("DESCRIPTION:");
    }

    @Test
    void should_escapeSpecialCharacters_when_summaryContainsCommaOrSemicolon() {
        IcsEvent event = new IcsEvent(100L, LocalDateTime.of(2026, 7, 22, 13, 0),
            LocalDateTime.of(2026, 7, 22, 14, 0), "Saç, Sakal; Bakım", null, null, false);

        String ics = IcsGenerator.generate(event);

        assertThat(ics).contains("SUMMARY:Saç\\, Sakal\\; Bakım");
    }

    @Test
    void should_produceValidVCalendarStructure_when_generated() {
        String ics = IcsGenerator.generate(confirmedEvent());

        assertThat(ics).startsWith("BEGIN:VCALENDAR");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("BEGIN:VEVENT");
        assertThat(ics).contains("END:VEVENT");
        assertThat(ics).contains("VERSION:2.0");
    }
}
