package com.asaf.appointment_service.entity.plan;

import java.util.Map;

/**
 * Plan özellik adlarının Türkçe karşılıkları
 */
public final class PlanFeatureNames {
    
    private PlanFeatureNames() {}
    
    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
        // Bildirim özellikleri
        Map.entry(PlanFeature.WHATSAPP_NOTIFICATIONS, "WhatsApp bildirimleri"),
        Map.entry(PlanFeature.SMS_NOTIFICATIONS, "SMS bildirimleri"),
        
        // İş yönetimi özellikleri
        Map.entry(PlanFeature.PACKAGE_MANAGEMENT, "Paket yönetimi"),
        Map.entry(PlanFeature.NO_SHOW_TRACKING, "No-show takibi"),
        Map.entry(PlanFeature.CUSTOMER_NOTES, "Müşteri notları"),
        
        // Raporlama özellikleri
        Map.entry(PlanFeature.BASIC_REPORTS, "Temel raporlar"),
        Map.entry(PlanFeature.ADVANCED_REPORTS, "Gelişmiş raporlar"),
        Map.entry(PlanFeature.EXPORT_REPORTS, "Rapor dışa aktarma"),
        
        // Online randevu sayfası
        Map.entry(PlanFeature.ONLINE_BOOKING, "Online randevu sayfası"),
        Map.entry(PlanFeature.REMOVE_BRANDING, "Marka kaldırma"),
        Map.entry(PlanFeature.CUSTOM_DOMAIN, "Özel domain"),
        
        // Destek ve API
        Map.entry(PlanFeature.PRIORITY_SUPPORT, "Öncelikli destek"),
        Map.entry(PlanFeature.SUPPORT_24_7, "7/24 destek"),
        Map.entry(PlanFeature.API_ACCESS, "API erişimi")
    );
    
    public static String getDisplayName(String featureCode) {
        return DISPLAY_NAMES.getOrDefault(featureCode, featureCode);
    }
    
    public static Map<String, String> getAllDisplayNames() {
        return Map.copyOf(DISPLAY_NAMES);
    }
}
