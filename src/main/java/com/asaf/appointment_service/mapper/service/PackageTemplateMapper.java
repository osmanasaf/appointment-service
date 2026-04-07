package com.asaf.appointment_service.mapper.service;

import com.asaf.appointment_service.dto.service.CreatePackageTemplateRequest;
import com.asaf.appointment_service.dto.service.PackageTemplateResponse;
import com.asaf.appointment_service.entity.service.PackageTemplate;
import java.util.List;

public class PackageTemplateMapper {

    private PackageTemplateMapper() {}

    public static PackageTemplate toEntity(CreatePackageTemplateRequest request) {
        return PackageTemplate.builder()
            .businessId(request.businessId())
            .serviceId(request.serviceId())
            .name(request.name())
            .totalSessions(request.totalSessions())
            .price(request.price())
            .build();
    }

    public static PackageTemplateResponse toResponse(PackageTemplate entity) {
        return new PackageTemplateResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getServiceId(),
            entity.getName(),
            entity.getTotalSessions(),
            entity.getPrice(),
            entity.isActive()
        );
    }

    public static List<PackageTemplateResponse> toResponseList(List<PackageTemplate> entities) {
        return entities.stream().map(PackageTemplateMapper::toResponse).toList();
    }
}
