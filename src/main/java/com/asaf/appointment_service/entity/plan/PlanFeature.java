package com.asaf.appointment_service.entity.plan;

/**
 * Plan özellik kodları.
 * Her plan hangi özelliklere sahip olduğunu features listesinde tutar.
 */
public final class PlanFeature {
    
    private PlanFeature() {}
    
    // Bildirim özellikleri
    public static final String WHATSAPP_NOTIFICATIONS = "WHATSAPP_NOTIFICATIONS";
    public static final String SMS_NOTIFICATIONS = "SMS_NOTIFICATIONS";
    
    // İş yönetimi özellikleri
    public static final String PACKAGE_MANAGEMENT = "PACKAGE_MANAGEMENT";
    public static final String NO_SHOW_TRACKING = "NO_SHOW_TRACKING";
    public static final String CUSTOMER_NOTES = "CUSTOMER_NOTES";
    
    // Raporlama özellikleri
    public static final String BASIC_REPORTS = "BASIC_REPORTS";
    public static final String ADVANCED_REPORTS = "ADVANCED_REPORTS";
    public static final String EXPORT_REPORTS = "EXPORT_REPORTS";
    
    // Online randevu sayfası
    public static final String ONLINE_BOOKING = "ONLINE_BOOKING";
    public static final String REMOVE_BRANDING = "REMOVE_BRANDING";
    public static final String CUSTOM_DOMAIN = "CUSTOM_DOMAIN";
    
    // Destek ve API
    public static final String PRIORITY_SUPPORT = "PRIORITY_SUPPORT";
    public static final String SUPPORT_24_7 = "SUPPORT_24_7";
    public static final String API_ACCESS = "API_ACCESS";

    // Müşteri çekme özellikleri (PRO+)
    public static final String WAITLIST = "WAITLIST";
    public static final String CUSTOMER_REVIEWS = "CUSTOMER_REVIEWS";
    public static final String DEPOSIT_PAYMENTS = "DEPOSIT_PAYMENTS";
    public static final String CALENDAR_SYNC = "CALENDAR_SYNC";
    public static final String MINI_WEBSITE = "MINI_WEBSITE";
}
