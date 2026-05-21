package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeFromInviteRequest(
    @NotBlank(message = "E-posta gereklidir")
    @Email(message = "Geçerli bir e-posta giriniz")
    @Size(max = ValidationPatterns.EMAIL_MAX_LENGTH, message = "E-posta en fazla 254 karakter olabilir")
    String email,

    @NotBlank(message = "Çalışan adı zorunludur")
    @Size(min = 2, max = 100, message = "Çalışan adı 2-100 karakter arasında olmalıdır")
    String name,

    @Size(max = 100, message = "Ünvan en fazla 100 karakter olabilir")
    String title,

    @Size(max = 500, message = "Mesaj en fazla 500 karakter olabilir")
    String message
) {
    public CreateEmployeeFromInviteRequest {
        if (title != null && title.isBlank()) {
            title = null;
        }
        if (message != null && message.isBlank()) {
            message = null;
        }
    }
}
