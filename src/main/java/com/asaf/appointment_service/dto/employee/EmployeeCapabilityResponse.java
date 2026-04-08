package com.asaf.appointment_service.dto.employee;

import com.asaf.appointment_service.entity.employee.SkillLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeCapabilityResponse(
    Long id,
    Long employeeId,
    Long serviceId,
    String serviceName,
    Integer customDurationMinutes,
    BigDecimal customPrice,
    SkillLevel skillLevel,
    boolean active,
    LocalDateTime createdAt
) {}
