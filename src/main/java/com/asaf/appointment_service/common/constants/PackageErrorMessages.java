package com.asaf.appointment_service.common.constants;

public final class PackageErrorMessages {

    public static final String PACKAGE_NOT_FOUND = "Paket bulunamadı: ";
    public static final String SESSION_NOT_FOUND = "Paket seansı bulunamadı: ";
    public static final String PACKAGE_EXPIRED = "Paket süresi dolmuş";
    public static final String NO_REMAINING_SESSIONS = "Pakette kalan seans yok";
    public static final String PACKAGE_NOT_ELIGIBLE = "Bu hizmet için paket tanımlanamaz";
    public static final String TEMPLATE_WRONG_BUSINESS = "Şablon bu işletmeye ait değil";
    public static final String SESSION_NOT_PENDING = "Sadece bekleyen seansa randevu atanabilir";
    public static final String SESSION_NOT_SCHEDULED = "Sadece randevu atanmış seanstan randevu kaldırılabilir";
    public static final String PACKAGE_RECORD_NOT_FOUND = "Paket bulunamadı";

    private PackageErrorMessages() {
    }
}
