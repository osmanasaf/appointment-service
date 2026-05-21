package com.asaf.appointment_service.mapper.employee;

import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.employee.EmployeeDetailResponse;
import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import com.asaf.appointment_service.entity.employee.Employee;
import java.util.List;

public class EmployeeMapper {

    private EmployeeMapper() {}

    public static Employee toEntity(Long businessId, CreateEmployeeRequest request) {
        return Employee.builder()
            .businessId(businessId)
            .name(request.name())
            .phone(request.phoneCountryCode(), request.phoneNumber())
            .email(request.email())
            .bio(request.bio())
            .title(request.title())
            .owner(request.owner())
            .build();
    }

    public static EmployeeResponse toResponse(Employee entity) {
        return new EmployeeResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getName(),
            entity.getTitle(),
            entity.getFormattedPhoneNumber(),
            entity.getEmail(),
            entity.getBio(),
            entity.getProfileImageUrl(),
            entity.getStatus(),
            entity.isOwner(),
            entity.getExperienceYears(),
            entity.isAcceptsOnlineBooking(),
            entity.getCreatedAt()
        );
    }

    public static EmployeeDetailResponse toDetailResponse(Employee entity) {
        return new EmployeeDetailResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getName(),
            entity.getTitle(),
            entity.getFormattedPhoneNumber(),
            entity.getEmail(),
            entity.getBio(),
            entity.getProfileImageUrl(),
            entity.getStatus(),
            entity.isOwner(),
            entity.getExperienceYears(),
            entity.isAcceptsOnlineBooking(),
            entity.getCreatedAt()
        );
    }

    public static List<EmployeeResponse> toResponseList(List<Employee> entities) {
        return entities.stream().map(EmployeeMapper::toResponse).toList();
    }
}
