package com.asaf.appointment_service.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSanitizerTest {

    @Test
    void should_returnNull_when_inputIsNull() {
        assertThat(TextSanitizer.stripHtml(null, 100)).isNull();
    }

    @Test
    void should_returnNull_when_inputIsBlankAfterStripping() {
        assertThat(TextSanitizer.stripHtml("   ", 100)).isNull();
    }

    @Test
    void should_removeHtmlTags_when_inputContainsMarkup() {
        String result = TextSanitizer.stripHtml("<script>alert('x')</script>Harika hizmet!", 100);

        assertThat(result).isEqualTo("alert('x')Harika hizmet!");
    }

    @Test
    void should_trimWhitespace_when_inputHasLeadingTrailingSpaces() {
        assertThat(TextSanitizer.stripHtml("  Çok memnun kaldım  ", 100)).isEqualTo("Çok memnun kaldım");
    }

    @Test
    void should_truncateToMaxLength_when_inputExceedsLimit() {
        String longText = "a".repeat(50);

        String result = TextSanitizer.stripHtml(longText, 10);

        assertThat(result).hasSize(10);
    }

    @Test
    void should_keepPlainText_when_noHtmlPresent() {
        assertThat(TextSanitizer.stripHtml("Süper bir deneyimdi, teşekkürler!", 100))
            .isEqualTo("Süper bir deneyimdi, teşekkürler!");
    }
}
