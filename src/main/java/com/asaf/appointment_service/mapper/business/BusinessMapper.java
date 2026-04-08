package com.asaf.appointment_service.mapper.business;

import com.asaf.appointment_service.dto.business.BusinessResponse;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import java.util.List;

public class BusinessMapper {

    private BusinessMapper() {}

    public static Business toEntity(CreateBusinessRequest request, String slug) {
        return Business.builder()
            .name(request.name())
            .slug(slug)
            .phone(request.phoneCountryCode(), request.phoneNumber())
            .email(request.email())
            .address(request.address())
            .description(request.description())
            .category(request.category())
            .build();
    }

    public static BusinessResponse toResponse(Business entity) {
        return new BusinessResponse(
            entity.getId(),
            entity.getName(),
            entity.getSlug(),
            entity.getFormattedPhoneNumber(),
            entity.getEmail(),
            entity.getAddress(),
            entity.getDescription(),
            entity.getLogoUrl(),
            entity.getCategory(),
            entity.getCategory() != null ? entity.getCategory().getLabel() : null,
            entity.isAutoConfirm(),
            entity.getReminderHoursBefore(),
            entity.isSameDayBookingAllowed(),
            entity.getMaxAdvanceBookingDays(),
            entity.getMaxAppointmentsPerCustomerPerDay(),
            entity.isWhatsappSendPrice(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    public static List<BusinessResponse> toResponseList(List<Business> entities) {
        return entities.stream().map(BusinessMapper::toResponse).toList();
    }
}
