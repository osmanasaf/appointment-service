package com.asaf.appointment_service.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Utility for generating URL-friendly slugs from text.
 * Turkish character support included.
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    /**
     * Generates a unique slug from the given input.
     * 
     * @param input The text to generate slug from
     * @param existsCheck Function to check if slug already exists
     * @return Unique slug
     */
    public String generate(String input, Function<String, Boolean> existsCheck) {
        String slug = toSlug(input);
        return ensureUnique(slug, existsCheck);
    }

    /**
     * Converts text to a basic slug (may not be unique).
     */
    public String toSlug(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Slug için girdi boş olamaz");
        }

        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.forLanguageTag("tr"));
        
        // Remove leading/trailing dashes
        slug = slug.replaceAll("^-+|-+$", "");
        
        return slug;
    }

    private String ensureUnique(String baseSlug, Function<String, Boolean> existsCheck) {
        String slug = baseSlug;
        int counter = 1;
        
        while (existsCheck.apply(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }
}
