package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.notification.MessageTemplateResponse;
import com.asaf.appointment_service.entity.notification.MessageTemplate;
import com.asaf.appointment_service.entity.notification.MessageTemplateType;
import com.asaf.appointment_service.mapper.notification.MessageTemplateMapper;
import com.asaf.appointment_service.service.notification.MessageTemplateService;
import com.asaf.appointment_service.service.notification.MessageTemplateService.MessageTemplateUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/message-templates")
public class MessageTemplateController {

    private final MessageTemplateService templateService;
    private final CurrentUserService currentUserService;

    public MessageTemplateController(MessageTemplateService templateService,
                                     CurrentUserService currentUserService) {
        this.templateService = templateService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageTemplateResponse>>> getAll() {
        Long businessId = currentUserService.getCurrentBusinessId();
        templateService.initializeDefaultTemplates(businessId);
        List<MessageTemplate> templates = templateService.getTemplatesForBusiness(businessId);
        return ResponseEntity.ok(ApiResponse.success(MessageTemplateMapper.toResponseList(templates), "Şablonlar getirildi"));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<MessageTemplateResponse>> getByType(@PathVariable MessageTemplateType type) {
        Long businessId = currentUserService.getCurrentBusinessId();
        MessageTemplate template = templateService.getTemplateOrDefault(businessId, type);
        return ResponseEntity.ok(ApiResponse.success(MessageTemplateMapper.toResponse(template), "Şablon getirildi"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageTemplateResponse>> update(
            @PathVariable Long id,
            @RequestBody MessageTemplateUpdateRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        MessageTemplate updated = templateService.updateTemplate(businessId, id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageTemplateMapper.toResponse(updated), "Şablon güncellendi"));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ApiResponse<MessageTemplateResponse>> reset(@PathVariable Long id) {
        Long businessId = currentUserService.getCurrentBusinessId();
        MessageTemplate reset = templateService.resetToDefault(businessId, id);
        return ResponseEntity.ok(ApiResponse.success(MessageTemplateMapper.toResponse(reset), "Şablon varsayılana döndürüldü"));
    }
}
