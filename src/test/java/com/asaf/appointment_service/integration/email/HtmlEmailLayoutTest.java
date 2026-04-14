package com.asaf.appointment_service.integration.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlEmailLayoutTest {

    @Test
    void escape_shouldEncodeHtmlEntities() {
        assertThat(HtmlEmailLayout.escape("<script>")).isEqualTo("&lt;script&gt;");
        assertThat(HtmlEmailLayout.escape("A & B")).isEqualTo("A &amp; B");
    }

    @Test
    void document_shouldIncludeStructureAndFooterYear() {
        String html = HtmlEmailLayout.document(
            "pre",
            "Kicker",
            "Headline",
            "<p>Body</p>",
            "Footer line.");
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("Headline");
        assertThat(html).contains("Kicker");
        assertThat(html).contains("<p>Body</p>");
        assertThat(html).contains("Footer line.");
        assertThat(html).contains("randevum.pro");
        assertThat(html).matches("(?s).*Randevum.*");
    }

    @Test
    void primaryButton_shouldContainHrefAndEscapedLabel() {
        String btn = HtmlEmailLayout.primaryButton("https://randevum.pro/x", "Go & run");
        assertThat(btn).contains("https://randevum.pro/x");
        assertThat(btn).contains("Go &amp; run");
    }
}
