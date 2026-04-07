package com.asaf.appointment_service.dto.customer;

import java.time.LocalDateTime;

public record CustomerResponse(
    Long id,
    Long businessId,
    String name,
    String phoneNumber,
    String email,
    String notes,
    String address,
    String nationalIdMasked,
    int noShowCount,
    boolean blacklisted,
    String blacklistReason,
    boolean risky,
    boolean noShowForcesManualApproval,
    LocalDateTime createdAt
) {}
