package com.asaf.appointment_service.common.exception;

public enum ErrorCode {
    INVALID_CREDENTIALS("Geçersiz kimlik bilgileri."),
    EMAIL_NOT_VERIFIED("E-posta adresiniz doğrulanmamış."),
    INVALID_OTP("Geçersiz doğrulama kodu."),
    OTP_BLOCKED("Çok fazla hatalı deneme. Lütfen daha sonra tekrar deneyin."),
    VALIDATION_ERROR("Girilen veriler geçersiz."),
    NOT_FOUND("Kayıt bulunamadı."),
    BUSINESS_NOT_FOUND("İşletme bulunamadı."),
    EMPLOYEE_NOT_FOUND("Çalışan bulunamadı."),
    CUSTOMER_NOT_FOUND("Müşteri bulunamadı."),
    SERVICE_NOT_FOUND("Hizmet bulunamadı."),
    APPOINTMENT_NOT_FOUND("Randevu bulunamadı."),
    PLAN_NOT_FOUND("Plan bulunamadı."),
    PLAN_LIMIT_EXCEEDED("Plan limitiniz aşıldı."),
    SLOT_NOT_AVAILABLE("Seçilen zaman dilimi müsait değil."),
    EMPLOYEE_ON_LEAVE("Çalışan bu tarihte izinli."),
    CUSTOMER_DAY_LIMIT_EXCEEDED("Günlük randevu limitiniz aşıldı."),
    INVALID_REQUEST_BODY("İstek gövdesi okunamadı."),
    MISSING_PARAMETER("Zorunlu parametre eksik."),
    INVALID_PARAMETER("Parametre tipi hatalı."),
    BAD_REQUEST("Geçersiz istek."),
    UNAUTHORIZED("Oturum açmanız gerekiyor."),
    ACCESS_DENIED("Bu işlem için yetkiniz yok."),
    FORBIDDEN("Erişim engellendi."),
    CONFLICT("Bu kayıt başka bir kayıtla çakışıyor."),
    DATA_CONFLICT("Veri tutarlılığı hatası."),
    FILE_TOO_LARGE("Dosya boyutu çok büyük."),
    INTERNAL_ERROR("Beklenmeyen bir hata oluştu.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getCode() {
        return this.name();
    }
}
