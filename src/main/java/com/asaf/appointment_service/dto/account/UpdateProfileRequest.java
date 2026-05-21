package com.asaf.appointment_service.dto.account;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Ad soyad zorunludur")
    @Size(max = 120, message = "Ad soyad en fazla 120 karakter olabilir")
    String name,

    @Pattern(regexp = ValidationPatterns.PHONE_FLEXIBLE_REGEX, message = ValidationPatterns.PHONE_MESSAGE)
    String phoneNumber
) {}
