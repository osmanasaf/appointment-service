package com.asaf.appointment_service.entity.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"business_id", "type"})
})
public class MessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageTemplateType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String bodyTemplate;

    @Column(length = 100)
    private String headerTemplate;

    @Column(length = 100)
    private String footerTemplate;

    @Column(nullable = false)
    private boolean showConfirmButton = true;

    @Column(nullable = false)
    private boolean showCancelButton = true;

    @Column(nullable = false)
    private boolean showRescheduleButton = true;

    @Column(length = 30)
    private String confirmButtonText = "✅ Geliyorum";

    @Column(length = 30)
    private String cancelButtonText = "❌ İptal Et";

    @Column(length = 30)
    private String rescheduleButtonText = "🔄 Ertele";

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public MessageTemplate() {}

    public static MessageTemplate createDefault(Long businessId, MessageTemplateType type) {
        MessageTemplate template = new MessageTemplate();
        template.businessId = businessId;
        template.type = type;
        
        switch (type) {
            case APPOINTMENT_REMINDER -> {
                template.name = "Randevu Hatırlatma";
                template.headerTemplate = "📅 Randevu Hatırlatma";
                template.bodyTemplate = """
                    Merhaba {{customer_name}},
                    
                    {{business_name}} işletmesinde {{date}} tarihinde saat {{time}}'de {{service_name}} randevunuz bulunmaktadır.
                    
                    Lütfen katılım durumunuzu belirtin.""";
                template.footerTemplate = "{{business_name}}";
            }
            case APPOINTMENT_CONFIRMATION -> {
                template.name = "Randevu Onayı";
                template.headerTemplate = "✅ Randevu Onaylandı";
                template.bodyTemplate = """
                    Merhaba {{customer_name}},
                    
                    {{date}} tarihinde saat {{time}}'de {{service_name}} randevunuz onaylanmıştır.
                    
                    Görüşmek üzere!""";
                template.footerTemplate = "{{business_name}}";
                template.showConfirmButton = false;
                template.showCancelButton = false;
                template.showRescheduleButton = false;
            }
            case APPOINTMENT_CANCELLED -> {
                template.name = "Randevu İptali";
                template.headerTemplate = "❌ Randevu İptal Edildi";
                template.bodyTemplate = """
                    Merhaba {{customer_name}},
                    
                    {{date}} tarihindeki {{service_name}} randevunuz iptal edilmiştir.
                    
                    Yeni randevu almak için bizimle iletişime geçebilirsiniz.""";
                template.footerTemplate = "{{business_name}}";
                template.showConfirmButton = false;
                template.showCancelButton = false;
                template.showRescheduleButton = false;
            }
            case RESCHEDULE_REQUEST_RECEIVED -> {
                template.name = "Erteleme Talebi Alındı";
                template.headerTemplate = "🔄 Erteleme Talebi";
                template.bodyTemplate = """
                    Merhaba {{customer_name}},
                    
                    {{date}} tarihindeki {{service_name}} randevunuz için erteleme talebiniz alınmıştır.
                    
                    En kısa sürede sizinle iletişime geçeceğiz.""";
                template.footerTemplate = "{{business_name}}";
                template.showConfirmButton = false;
                template.showCancelButton = false;
                template.showRescheduleButton = false;
            }
        }
        
        return template;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    public MessageTemplateType getType() { return type; }
    public void setType(MessageTemplateType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }

    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }

    public String getFooterTemplate() { return footerTemplate; }
    public void setFooterTemplate(String footerTemplate) { this.footerTemplate = footerTemplate; }

    public boolean isShowConfirmButton() { return showConfirmButton; }
    public void setShowConfirmButton(boolean showConfirmButton) { this.showConfirmButton = showConfirmButton; }

    public boolean isShowCancelButton() { return showCancelButton; }
    public void setShowCancelButton(boolean showCancelButton) { this.showCancelButton = showCancelButton; }

    public boolean isShowRescheduleButton() { return showRescheduleButton; }
    public void setShowRescheduleButton(boolean showRescheduleButton) { this.showRescheduleButton = showRescheduleButton; }

    public String getConfirmButtonText() { return confirmButtonText; }
    public void setConfirmButtonText(String confirmButtonText) { this.confirmButtonText = confirmButtonText; }

    public String getCancelButtonText() { return cancelButtonText; }
    public void setCancelButtonText(String cancelButtonText) { this.cancelButtonText = cancelButtonText; }

    public String getRescheduleButtonText() { return rescheduleButtonText; }
    public void setRescheduleButtonText(String rescheduleButtonText) { this.rescheduleButtonText = rescheduleButtonText; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
