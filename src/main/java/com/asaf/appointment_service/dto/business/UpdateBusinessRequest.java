package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateBusinessRequest(
    @Size(min = 2, max = 100, message = "İşletme adı 2-100 karakter arasında olmalıdır")
    String name,

    BusinessCategory category,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    String address,

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    String description,

    @Size(max = 2048, message = "Logo URL'si çok uzun")
    String logoUrl,

    Boolean autoConfirm,

    @Min(value = 1, message = "Hatırlatma en az 1 saat önce olmalıdır")
    @Max(value = 72, message = "Hatırlatma en fazla 72 saat önce olabilir")
    Integer reminderHoursBefore,

    Boolean sameDayBookingAllowed,

    @Min(value = 1, message = "İleri tarihli randevu en az 1 gün olmalıdır")
    @Max(value = 90, message = "İleri tarihli randevu en fazla 90 gün olabilir")
    Integer maxAdvanceBookingDays,

    @Min(value = 1, message = "Müşteri başına günlük randevu en az 1 olmalıdır")
    @Max(value = 10, message = "Müşteri başına günlük randevu en fazla 10 olabilir")
    Integer maxAppointmentsPerCustomerPerDay,

    Boolean whatsappSendPrice,

    @Size(max = 500, message = "Google yorum linki çok uzun")
    String googleReviewUrl
) {}
