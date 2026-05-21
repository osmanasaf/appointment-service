package com.asaf.appointment_service.dto.account;

import com.asaf.appointment_service.entity.user.UserRole;

public record ProfileResponse(
    Long id,
    String email,
    String name,
    String phoneNumber,
    Long businessId,
    Long employeeId,
    UserRole role,
    String businessSlug,
    boolean emailVerified,
    boolean twoFactorEnabled
) {}
