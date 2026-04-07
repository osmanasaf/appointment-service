package com.asaf.appointment_service.mapper.service;

import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.dto.service.ServiceResponse;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import java.util.List;

public class ServiceMapper {

    private ServiceMapper() {}

    public static ServiceDefinition toEntity(CreateServiceRequest request) {
        return ServiceDefinition.builder()
            .businessId(request.businessId())
            .name(request.name())
            .description(request.description())
            .durationMinutes(request.durationMinutes())
            .buffers(request.bufferBeforeMinutes(), request.bufferAfterMinutes())
            .price(request.price())
            .requiresConfirmation(request.requiresConfirmation())
            .sameDayBookingAllowed(request.sameDayBookingAllowed())
            .packageEligible(request.packageEligible())
            .build();
    }

    public static ServiceResponse toResponse(ServiceDefinition entity) {
        return new ServiceResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getName(),
            entity.getDescription(),
            entity.getDurationMinutes(),
            entity.getBufferBeforeMinutes(),
            entity.getBufferAfterMinutes(),
            entity.totalDurationMinutes(),
            entity.getPrice(),
            entity.getCurrency(),
            entity.isRequiresConfirmation(),
            entity.isSameDayBookingAllowed(),
            entity.isPackageEligible(),
            entity.isActive(),
            entity.getDisplayOrder(),
            entity.getCreatedAt()
        );
    }

    public static List<ServiceResponse> toResponseList(List<ServiceDefinition> entities) {
        return entities.stream().map(ServiceMapper::toResponse).toList();
    }
}
