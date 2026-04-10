package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
import com.asaf.appointment_service.entity.notification.MessageTemplate;
import com.asaf.appointment_service.entity.notification.MessageTemplateType;
import com.asaf.appointment_service.service.notification.MessageTemplateService;
import com.asaf.appointment_service.service.notification.MessageTemplateService.MessageTemplateUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/message-templates")
public class MessageTemplateController {

    private final MessageTemplateService templateService;
    private final AuthenticatedBusinessService authBusinessService;

    public MessageTemplateController(MessageTemplateService templateService,
                                     AuthenticatedBusinessService authBusinessService) {
        this.templateService = templateService;
        this.authBusinessService = authBusinessService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageTemplate>>> getAll() {
        Long businessId = authBusinessService.getCurrentBusinessId();
        templateService.initializeDefaultTemplates(businessId);
        List<MessageTemplate> templates = templateService.getTemplatesForBusiness(businessId);
        return ResponseEntity.ok(ApiResponse.success(templates, "Şablonlar getirildi"));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<MessageTemplate>> getByType(@PathVariable MessageTemplateType type) {
        Long businessId = authBusinessService.getCurrentBusinessId();
        MessageTemplate template = templateService.getTemplateOrDefault(businessId, type);
        return ResponseEntity.ok(ApiResponse.success(template, "Şablon getirildi"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageTemplate>> update(
            @PathVariable Long id,
            @RequestBody MessageTemplateUpdateRequest request) {
        Long businessId = authBusinessService.getCurrentBusinessId();
        MessageTemplate updated = templateService.updateTemplate(businessId, id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Şablon güncellendi"));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ApiResponse<MessageTemplate>> reset(@PathVariable Long id) {
        Long businessId = authBusinessService.getCurrentBusinessId();
        MessageTemplate reset = templateService.resetToDefault(businessId, id);
        return ResponseEntity.ok(ApiResponse.success(reset, "Şablon varsayılana döndürüldü"));
    }
}
