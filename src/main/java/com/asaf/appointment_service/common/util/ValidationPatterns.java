package com.asaf.appointment_service.common.util;

public final class ValidationPatterns {

    private ValidationPatterns() {}

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;

    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*\\d).{8,128}$";
    public static final String PASSWORD_MESSAGE =
        "Şifre en az 8 karakter, 1 büyük harf ve 1 rakam içermelidir";

    public static final String PHONE_E164_REGEX = "^\\+?[1-9]\\d{6,14}$";
    public static final String PHONE_TR_LOCAL_REGEX = "^5\\d{9}$";
    public static final String PHONE_FLEXIBLE_REGEX =
        "^(\\+?[1-9]\\d{6,14}|5\\d{9})$";
    public static final String PHONE_MESSAGE =
        "Geçerli bir telefon numarası giriniz (örn: 5XXXXXXXXX veya +905XXXXXXXXX)";

    public static final String EMAIL_REGEX =
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$";
    public static final int EMAIL_MAX_LENGTH = 254;

    public static final String NAME_REGEX = "^[\\p{L}\\p{M}\\s'\\-.]{2,100}$";
    public static final String NAME_MESSAGE =
        "Ad 2-100 karakter olmalı; harf, boşluk, kesme, tire içerebilir";

    public static final String NATIONAL_ID_REGEX = "\\d{11}";

    public static final String OTP_REGEX = "^\\d{6}$";

    public static final String URL_REGEX =
        "^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+([/?#][^\\s]*)?$";

    public static final String INSTAGRAM_HANDLE_REGEX = "^[A-Za-z0-9._]{1,30}$";
}
