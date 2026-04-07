package com.asaf.appointment_service.entity.appointment;

/**
 * Randevunun nereden alındığını belirtir.
 * Telefon, manuel (yerinde) ve online randevular tek takvimde senkron tutulur.
 */
public enum AppointmentSource {
    /** Müşteri web/uygulama üzerinden kendisi aldı */
    ONLINE,
    /** Personel telefonla arayan müşteri için sisteme kaydetti */
    PHONE,
    /** Müşteri yerinde (manuel) randevu aldı */
    WALK_IN,
    /** Diğer (dış sistem, toplu aktarım vb.) */
    OTHER
}
