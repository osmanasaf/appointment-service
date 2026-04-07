package com.asaf.appointment_service.mapper.customer;

import com.asaf.appointment_service.dto.customer.PackageSessionResponse;
import com.asaf.appointment_service.entity.customer.PackageSession;
import java.util.List;

public class PackageSessionMapper {

    private PackageSessionMapper() {}

    public static PackageSessionResponse toResponse(PackageSession entity) {
        return new PackageSessionResponse(
            entity.getId(),
            entity.getCustomerPackageId(),
            entity.getSessionNumber(),
            entity.getStatus(),
            entity.getAppointmentId(),
            entity.getScheduledAt(),
            entity.getNotes()
        );
    }

    public static List<PackageSessionResponse> toResponseList(List<PackageSession> entities) {
        return entities.stream().map(PackageSessionMapper::toResponse).toList();
    }
}
