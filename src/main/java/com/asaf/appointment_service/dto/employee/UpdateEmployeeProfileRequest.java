package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeProfileRequest(
    @NotBlank(message = "Çalışan adı zorunludur")
    @Size(min = 2, max = 100, message = "Çalışan adı 2-100 karakter arasında olmalıdır")
    String name,

    @Size(max = 100, message = "Ünvan en fazla 100 karakter olabilir")
    String title,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @Size(max = 500, message = "Biyografi en fazla 500 karakter olabilir")
    String bio,

    @Size(max = 2048, message = "Profil resmi URL'si çok uzun")
    String profileImageUrl,

    @Min(value = 0, message = "Deneyim yılı negatif olamaz")
    @Max(value = 80, message = "Deneyim yılı geçersiz")
    Integer experienceYears,

    boolean acceptsOnlineBooking
) {
    public UpdateEmployeeProfileRequest {
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
