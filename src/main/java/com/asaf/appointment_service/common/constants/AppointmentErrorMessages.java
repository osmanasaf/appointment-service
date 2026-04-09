package com.asaf.appointment_service.common.constants;

public final class AppointmentErrorMessages {

    public static final String APPOINTMENT_NOT_FOUND = "Randevu bulunamadı: ";
    public static final String SLOT_NOT_AVAILABLE = "Seçilen saatte başka bir randevu mevcut";
    public static final String EMPLOYEE_CLOSED_DAY = "Çalışan bu gün çalışmamaktadır";
    public static final String EMPLOYEE_OUTSIDE_HOURS = "Seçilen saat çalışanın mesai saatleri dışında";
    public static final String NO_LINKED_SESSION = "Randevuya bağlı paket seansı yok";
    public static final String STAFF_SOURCE_INVALID =
            "Staff randevusu sadece PHONE veya WALK_IN kaynağı ile oluşturulabilir";

    private AppointmentErrorMessages() {
    }
}
