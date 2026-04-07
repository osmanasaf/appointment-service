package com.asaf.appointment_service.domain.valueobject;

import java.util.regex.Pattern;

public record PhoneNumber(String countryCode, String number) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^\\+[0-9]{1,3}$");

    public PhoneNumber {
        if (countryCode == null || countryCode.isBlank()) {
            countryCode = "+90";
        }
        if (!COUNTRY_CODE_PATTERN.matcher(countryCode).matches()) {
            throw new IllegalArgumentException("Geçersiz ülke kodu: " + countryCode);
        }
        if (number == null || !PHONE_PATTERN.matcher(number).matches()) {
            throw new IllegalArgumentException("Telefon numarası 10 haneli olmalıdır");
        }
    }

    public static PhoneNumber of(String countryCode, String number) {
        return new PhoneNumber(countryCode, number);
    }

    public static PhoneNumber ofTurkey(String number) {
        return new PhoneNumber("+90", number);
    }

    public static PhoneNumber parse(String fullNumber) {
        if (fullNumber == null || fullNumber.isBlank()) {
            throw new IllegalArgumentException("Telefon numarası boş olamaz");
        }
        String cleaned = fullNumber.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+")) {
            int splitIndex = cleaned.length() - 10;
            return new PhoneNumber(cleaned.substring(0, splitIndex), cleaned.substring(splitIndex));
        }
        return new PhoneNumber("+90", cleaned.length() > 10 ? cleaned.substring(cleaned.length() - 10) : cleaned);
    }

    public String formatted() {
        return countryCode + number;
    }

    public String whatsAppFormat() {
        return countryCode.replace("+", "") + number;
    }

    public String displayFormat() {
        return String.format("%s (%s) %s %s %s",
            countryCode,
            number.substring(0, 3),
            number.substring(3, 6),
            number.substring(6, 8),
            number.substring(8, 10));
    }

    public String masked() {
        return countryCode + "****" + number.substring(number.length() - 4);
    }
}
