package com.asaf.appointment_service.mapper.waitlist;

import com.asaf.appointment_service.dto.waitlist.WaitlistEntryResponse;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;

public class WaitlistMapper {

    private WaitlistMapper() {}

    public static WaitlistEntryResponse toResponse(WaitlistEntry entity) {
        return new WaitlistEntryResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getCustomerId(),
            entity.getServiceId(),
            entity.getEmployeeId(),
            entity.getPreferredFrom(),
            entity.getPreferredUntil(),
            entity.getStatus(),
            entity.getNotifiedAt(),
            entity.getOfferedScheduledAt(),
            entity.getCreatedAt()
        );
    }
}
