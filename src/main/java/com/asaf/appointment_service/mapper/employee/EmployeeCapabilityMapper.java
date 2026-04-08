package com.asaf.appointment_service.mapper.employee;

import com.asaf.appointment_service.dto.employee.EmployeeCapabilityResponse;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import java.util.List;
import java.util.Map;

public class EmployeeCapabilityMapper {

    private EmployeeCapabilityMapper() {}

    public static EmployeeCapabilityResponse toResponse(EmployeeServiceCapability entity, String serviceName) {
        return new EmployeeCapabilityResponse(
            entity.getId(),
            entity.getEmployeeId(),
            entity.getServiceId(),
            serviceName,
            entity.getCustomDurationMinutes(),
            entity.getCustomPrice(),
            entity.getSkillLevel(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }

    public static List<EmployeeCapabilityResponse> toResponseList(
            List<EmployeeServiceCapability> entities,
            Map<Long, String> serviceNameById) {
        return entities.stream()
            .map(e -> toResponse(e, serviceNameById.getOrDefault(e.getServiceId(), "")))
            .toList();
    }
}
