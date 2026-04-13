package com.asaf.appointment_service.mapper.notification;

import com.asaf.appointment_service.dto.notification.MessageTemplateResponse;
import com.asaf.appointment_service.entity.notification.MessageTemplate;
import java.util.List;

public class MessageTemplateMapper {

    private MessageTemplateMapper() {}

    public static MessageTemplateResponse toResponse(MessageTemplate entity) {
        if (entity == null) {
            return null;
        }
        return new MessageTemplateResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getType(),
            entity.getName(),
            entity.getHeaderTemplate(),
            entity.getBodyTemplate(),
            entity.getFooterTemplate(),
            entity.isShowConfirmButton(),
            entity.isShowCancelButton(),
            entity.isShowRescheduleButton(),
            entity.getConfirmButtonText(),
            entity.getCancelButtonText(),
            entity.getRescheduleButtonText(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static List<MessageTemplateResponse> toResponseList(List<MessageTemplate> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(MessageTemplateMapper::toResponse).toList();
    }
}
