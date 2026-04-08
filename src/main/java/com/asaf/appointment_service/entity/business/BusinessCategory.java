package com.asaf.appointment_service.entity.business;

public enum BusinessCategory {

    // Sağlık & Klinik
    PSYCHOLOGIST("Psikolog"),
    PSYCHIATRIST("Psikiyatrist"),
    DIETITIAN("Diyetisyen"),
    NUTRITIONIST("Beslenme Uzmanı"),
    PHYSIOTHERAPIST("Fizyoterapist"),
    DOCTOR("Doktor / Klinik"),
    DENTIST("Diş Hekimi"),
    DERMATOLOGIST("Dermatoloji"),
    VETERINARY("Veteriner"),

    // Güzellik & Bakım
    BEAUTY_CENTER("Güzellik Merkezi"),
    HAIR_SALON("Kuaför / Berber"),
    SPA("Spa & Masaj"),
    NAIL_STUDIO("Tırnak Stüdyosu"),
    TATTOO_STUDIO("Dövme Stüdyosu"),
    AESTHETICS("Estetik & Cilt Bakımı"),

    // Fitness & Spor
    GYM("Spor Salonu"),
    PERSONAL_TRAINER("Kişisel Antrenör"),
    YOGA_PILATES("Yoga / Pilates Stüdyo"),
    SPORTS_CLUB("Spor Kulübü"),

    // Eğitim
    PRIVATE_TUTOR("Özel Ders"),
    LANGUAGE_SCHOOL("Dil Okulu"),
    MUSIC_SCHOOL("Müzik Okulu"),
    DRIVING_SCHOOL("Sürücü Kursu"),

    // Diğer
    OTHER("Diğer");

    private final String label;

    BusinessCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
