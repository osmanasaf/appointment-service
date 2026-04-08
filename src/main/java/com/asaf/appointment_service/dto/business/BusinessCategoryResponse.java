package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.entity.business.BusinessCategory;

public record BusinessCategoryResponse(
    BusinessCategory value,
    String label
) {
    public static BusinessCategoryResponse from(BusinessCategory category) {
        return new BusinessCategoryResponse(category, category.getLabel());
    }
}
