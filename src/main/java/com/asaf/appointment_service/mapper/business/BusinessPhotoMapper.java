package com.asaf.appointment_service.mapper.business;

import com.asaf.appointment_service.dto.business.BusinessPhotoResponse;
import com.asaf.appointment_service.entity.business.BusinessPhoto;
import java.util.List;

public class BusinessPhotoMapper {

    private BusinessPhotoMapper() {}

    public static BusinessPhotoResponse toResponse(BusinessPhoto entity) {
        return new BusinessPhotoResponse(entity.getId(), entity.getUrl(), entity.getCaption(), entity.getSortOrder());
    }

    public static List<BusinessPhotoResponse> toResponseList(List<BusinessPhoto> entities) {
        return entities.stream().map(BusinessPhotoMapper::toResponse).toList();
    }
}
