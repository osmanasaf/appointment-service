package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.notification.MessageTemplate;
import com.asaf.appointment_service.entity.notification.MessageTemplateType;
import com.asaf.appointment_service.repository.notification.MessageTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MessageTemplateService {

    private static final Logger log = LoggerFactory.getLogger(MessageTemplateService.class);

    private final MessageTemplateRepository templateRepository;

    public MessageTemplateService(MessageTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<MessageTemplate> getTemplatesForBusiness(Long businessId) {
        return templateRepository.findByBusinessIdAndActiveTrue(businessId);
    }

    public Optional<MessageTemplate> getTemplate(Long businessId, MessageTemplateType type) {
        return templateRepository.findByBusinessIdAndTypeAndActiveTrue(businessId, type);
    }

    public MessageTemplate getTemplateOrDefault(Long businessId, MessageTemplateType type) {
        return templateRepository.findByBusinessIdAndTypeAndActiveTrue(businessId, type)
            .orElseGet(() -> MessageTemplate.createDefault(businessId, type));
    }

    @Transactional
    public void initializeDefaultTemplates(Long businessId) {
        for (MessageTemplateType type : MessageTemplateType.values()) {
            if (!templateRepository.existsByBusinessIdAndType(businessId, type)) {
                MessageTemplate template = MessageTemplate.createDefault(businessId, type);
                templateRepository.save(template);
                log.info("Varsayılan şablon oluşturuldu: businessId={}, type={}", businessId, type);
            }
        }
    }

    @Transactional
    public MessageTemplate updateTemplate(Long businessId, Long templateId, MessageTemplateUpdateRequest request) {
        MessageTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Şablon bulunamadı: " + templateId));

        if (!template.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Bu şablona erişim yetkiniz yok");
        }

        if (request.name() != null) template.setName(request.name());
        if (request.bodyTemplate() != null) template.setBodyTemplate(request.bodyTemplate());
        if (request.headerTemplate() != null) template.setHeaderTemplate(request.headerTemplate());
        if (request.footerTemplate() != null) template.setFooterTemplate(request.footerTemplate());
        if (request.showConfirmButton() != null) template.setShowConfirmButton(request.showConfirmButton());
        if (request.showCancelButton() != null) template.setShowCancelButton(request.showCancelButton());
        if (request.showRescheduleButton() != null) template.setShowRescheduleButton(request.showRescheduleButton());
        if (request.confirmButtonText() != null) template.setConfirmButtonText(request.confirmButtonText());
        if (request.cancelButtonText() != null) template.setCancelButtonText(request.cancelButtonText());
        if (request.rescheduleButtonText() != null) template.setRescheduleButtonText(request.rescheduleButtonText());

        return templateRepository.save(template);
    }

    @Transactional
    public MessageTemplate resetToDefault(Long businessId, Long templateId) {
        MessageTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Şablon bulunamadı: " + templateId));

        if (!template.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Bu şablona erişim yetkiniz yok");
        }

        MessageTemplate defaultTemplate = MessageTemplate.createDefault(businessId, template.getType());
        
        template.setName(defaultTemplate.getName());
        template.setBodyTemplate(defaultTemplate.getBodyTemplate());
        template.setHeaderTemplate(defaultTemplate.getHeaderTemplate());
        template.setFooterTemplate(defaultTemplate.getFooterTemplate());
        template.setShowConfirmButton(defaultTemplate.isShowConfirmButton());
        template.setShowCancelButton(defaultTemplate.isShowCancelButton());
        template.setShowRescheduleButton(defaultTemplate.isShowRescheduleButton());
        template.setConfirmButtonText(defaultTemplate.getConfirmButtonText());
        template.setCancelButtonText(defaultTemplate.getCancelButtonText());
        template.setRescheduleButtonText(defaultTemplate.getRescheduleButtonText());

        return templateRepository.save(template);
    }

    public String renderTemplate(MessageTemplate template, Map<String, String> variables) {
        String body = template.getBodyTemplate();
        String header = template.getHeaderTemplate();
        String footer = template.getFooterTemplate();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            
            body = body.replace(placeholder, value);
            if (header != null) header = header.replace(placeholder, value);
            if (footer != null) footer = footer.replace(placeholder, value);
        }

        return body;
    }

    public record MessageTemplateUpdateRequest(
        String name,
        String bodyTemplate,
        String headerTemplate,
        String footerTemplate,
        Boolean showConfirmButton,
        Boolean showCancelButton,
        Boolean showRescheduleButton,
        String confirmButtonText,
        String cancelButtonText,
        String rescheduleButtonText
    ) {}
}
