package com.asaf.appointment_service.common.util;

import java.util.regex.Pattern;

public final class PhoneNumberUtil {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    private PhoneNumberUtil() {}

    public static String normalizeDigitsOnly(String phone) {
        if (phone == null) return "";
        return NON_DIGITS.matcher(phone).replaceAll("");
    }

    public static String maskForLog(String phone) {
        if (phone == null || phone.isEmpty()) return "***";
        String digits = normalizeDigitsOnly(phone);
        if (digits.length() <= 4) return "***";
        return "***" + digits.substring(digits.length() - 4);
    }

    public static String formatForWhatsApp(String countryCode, String phoneNumber) {
        String code = countryCode != null ? countryCode : "+90";
        String digits = normalizeDigitsOnly(code + phoneNumber);
        if (digits.startsWith("0")) {
            digits = "90" + digits.substring(1);
        }
        if (!digits.startsWith("90") && digits.length() == 10) {
            digits = "90" + digits;
        }
        return digits;
    }

    public static String formatWithCountryCode(String countryCode, String phoneNumber) {
        String code = countryCode != null ? countryCode : "+90";
        return code + normalizeDigitsOnly(phoneNumber);
    }
}
