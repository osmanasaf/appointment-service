package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BusinessCategoryResponse(
    @JsonProperty("code") BusinessCategory code,
    String label
) {
    public static BusinessCategoryResponse from(BusinessCategory category) {
        return new BusinessCategoryResponse(category, category.getLabel());
    }
}
