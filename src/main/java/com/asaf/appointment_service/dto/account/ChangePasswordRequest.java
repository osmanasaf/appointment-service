package com.asaf.appointment_service.dto.account;

import com.asaf.appointment_service.common.util.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
    @NotBlank(message = "Mevcut şifre zorunludur")
    String currentPassword,

    @NotBlank(message = "Yeni şifre zorunludur")
    @Pattern(regexp = ValidationPatterns.PASSWORD_REGEX, message = ValidationPatterns.PASSWORD_MESSAGE)
    String newPassword
) {}
