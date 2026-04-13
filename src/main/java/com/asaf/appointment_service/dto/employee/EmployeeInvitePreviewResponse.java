package com.asaf.appointment_service.dto.employee;

public record EmployeeInvitePreviewResponse(
    String email,
    String employeeName,
    String businessName
) {}
