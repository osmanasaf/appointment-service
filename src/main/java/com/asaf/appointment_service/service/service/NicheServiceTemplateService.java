package com.asaf.appointment_service.service.service;

import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NicheServiceTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NicheServiceTemplateService.class);

    private final ServiceDefinitionService serviceDefinitionService;

    public NicheServiceTemplateService(ServiceDefinitionService serviceDefinitionService) {
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @Transactional
    public void seedServicesForCategory(Long businessId, BusinessCategory category) {
        List<CreateServiceRequest> templates = resolveTemplates(category);
        if (templates.isEmpty()) return;

        for (CreateServiceRequest template : templates) {
            serviceDefinitionService.createService(businessId, template);
        }

        log.info("Niche service templates seeded: businessId={}, category={}, count={}",
            businessId, category, templates.size());
    }

    private List<CreateServiceRequest> resolveTemplates(BusinessCategory category) {
        return switch (category) {
            case HAIR_SALON, BEAUTY_CENTER -> hairSalonTemplates();
            case AESTHETICS -> aestheticsTemplates();
            case PSYCHOLOGIST -> psychologistTemplates();
            case DIETITIAN -> dietitianTemplates();
            default -> List.of();
        };
    }

    private List<CreateServiceRequest> hairSalonTemplates() {
        return List.of(
            service("Saç Kesimi", "Yıkama + kesim + fön dahil", 45, 10, 200),
            service("Saç Boyama (Tek Renk)", "Köke veya tüm saça uygulama", 120, 15, 450),
            service("Ombre / Balayage", "Doğal geçişli renklendirme teknikleri", 180, 15, 700),
            service("Fön & Şekillendirme", "Yıkama + fön + saç şekillendirme", 30, 5, 120),
            service("Saç Bakım Maskesi", "Keratin veya protein içerikli derin bakım", 60, 10, 250)
        );
    }

    private List<CreateServiceRequest> aestheticsTemplates() {
        return List.of(
            service("Cilt Analizi & Temel Bakım", "Derin temizlik, tonik, nemlendirici uygulama", 60, 15, 350),
            service("Hydrafacial", "Hydra peeling + nem serumu + maske", 75, 15, 550),
            service("Lazer Epilasyon (Tek Bölge)", "Üst dudak, kaş arası veya küçük bölge", 30, 10, 200),
            service("Kaş Dizaynı & Kalıcı Makyaj", "İplik + şekillendirme veya microblading", 45, 10, 300),
            service("Kirpik Lifting & Boya", "Doğal kirpikleri kaldırma ve boyama", 60, 10, 350)
        );
    }

    private List<CreateServiceRequest> psychologistTemplates() {
        return List.of(
            service("İlk Değerlendirme Görüşmesi", "Tanışma, şikayet analizi ve hedef belirleme", 60, 10, 600),
            service("Bireysel Terapi Seansı", "Bireysel psikoterapi görüşmesi", 50, 10, 500),
            service("Çift Terapisi", "İki bireylik çift terapisi seansı", 90, 10, 800)
        );
    }

    private List<CreateServiceRequest> dietitianTemplates() {
        return List.of(
            service("İlk Görüşme & Beslenme Analizi", "Vücut analizi, beslenme alışkanlığı değerlendirmesi", 60, 10, 400),
            service("Kişisel Diyet Planı Hazırlanması", "Hedefe yönelik haftalık beslenme programı", 45, 10, 350),
            service("Takip Seansı", "İlerleme değerlendirmesi ve plan güncellemesi", 30, 5, 250)
        );
    }

    private CreateServiceRequest service(String name, String description,
                                          int durationMinutes, int bufferAfter, int price) {
        return new CreateServiceRequest(
            name,
            description,
            durationMinutes,
            0,
            bufferAfter,
            BigDecimal.valueOf(price),
            false,
            true,
            true
        );
    }
}
