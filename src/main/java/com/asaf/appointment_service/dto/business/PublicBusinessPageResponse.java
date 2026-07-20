package com.asaf.appointment_service.dto.business;

import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import java.util.List;

public record PublicBusinessPageResponse(
    BusinessResponse business,
    boolean miniWebsiteEnabled,
    List<BusinessPhotoResponse> photos,
    List<EmployeeResponse> employeeCards,
    boolean showBranding
) {}
