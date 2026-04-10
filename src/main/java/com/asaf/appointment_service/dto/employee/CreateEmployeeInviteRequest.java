package com.asaf.appointment_service.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeInviteRequest(
    @NotNull(message = "Çalışan ID gereklidir")
    Long employeeId,
    
    @NotNull(message = "E-posta gereklidir")
    @Email(message = "Geçerli bir e-posta giriniz")
    String email
) {}
