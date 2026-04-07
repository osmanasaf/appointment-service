package com.asaf.appointment_service.mapper.customer;

import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.customer.CustomerResponse;
import com.asaf.appointment_service.dto.customer.PackageResponse;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import java.util.List;

public class CustomerMapper {

    private CustomerMapper() {}

    public static Customer toEntity(CreateCustomerRequest request) {
        return Customer.builder()
            .businessId(request.businessId())
            .name(request.name())
            .phone(request.phoneCountryCode(), request.phoneNumber())
            .email(request.email())
            .notes(request.notes())
            .address(request.address())
            .build();
    }

    public static CustomerResponse toResponse(Customer entity, EncryptionService encryptionService) {
        String nationalIdMasked = encryptionService != null && entity.getTcEncrypted() != null
            ? encryptionService.maskLastFour(entity.getTcEncrypted())
            : null;
        return new CustomerResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getName(),
            entity.getFormattedPhoneNumber(),
            entity.getEmail(),
            entity.getNotes(),
            entity.getAddress(),
            nationalIdMasked,
            entity.getNoShowCount(),
            entity.isBlacklisted(),
            entity.getBlacklistReason(),
            entity.isRisky(),
            entity.isNoShowForcesManualApproval(),
            entity.getCreatedAt()
        );
    }

    public static List<CustomerResponse> toResponseList(List<Customer> entities, EncryptionService encryptionService) {
        return entities.stream().map(e -> toResponse(e, encryptionService)).toList();
    }

    public static PackageResponse toPackageResponse(CustomerPackage entity) {
        return new PackageResponse(
            entity.getId(),
            entity.getCustomerId(),
            entity.getServiceId(),
            entity.getTemplateId(),
            entity.getName(),
            entity.getTotalSessions(),
            entity.getRemainingSessions(),
            entity.getExpiresAt(),
            entity.isActive(),
            entity.isExpired(),
            entity.isLowOnSessions()
        );
    }

    public static List<PackageResponse> toPackageResponseList(List<CustomerPackage> entities) {
        return entities.stream().map(CustomerMapper::toPackageResponse).toList();
    }
}
