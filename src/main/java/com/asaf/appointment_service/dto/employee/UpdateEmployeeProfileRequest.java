package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeProfileRequest(
    @NotBlank(message = "Çalışan adı zorunludur")
    @Size(min = 2, max = 100, message = "Çalışan adı 2-100 karakter arasında olmalıdır")
    String name,

    @Size(max = 100, message = "Ünvan en fazla 100 karakter olabilir")
    String title,

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    String email,

    @Size(max = 500, message = "Biyografi en fazla 500 karakter olabilir")
    String bio,

    String profileImageUrl,

    Integer experienceYears,

    boolean acceptsOnlineBooking
) {}
