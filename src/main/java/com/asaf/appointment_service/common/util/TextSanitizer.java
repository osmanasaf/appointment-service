package com.asaf.appointment_service.common.util;

import java.util.regex.Pattern;

/**
 * Kullanıcıdan alınan serbest metinleri (yorum vb.) public sayfada göstermeden
 * önce HTML/script enjeksiyonuna karşı sadeleştirir.
 */
public final class TextSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    private TextSanitizer() {}

    public static String stripHtml(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String withoutTags = HTML_TAG.matcher(input).replaceAll("");
        String withoutControlChars = CONTROL_CHARS.matcher(withoutTags).replaceAll("");
        String trimmed = withoutControlChars.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
