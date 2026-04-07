package com.asaf.appointment_service.service.service;

import com.asaf.appointment_service.dto.service.CreatePackageTemplateRequest;
import com.asaf.appointment_service.dto.service.PackageTemplateResponse;
import com.asaf.appointment_service.dto.service.UpdatePackageTemplateRequest;
import com.asaf.appointment_service.entity.service.PackageTemplate;
import com.asaf.appointment_service.mapper.service.PackageTemplateMapper;
import com.asaf.appointment_service.repository.service.PackageTemplateRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageTemplateService {

    private final PackageTemplateRepository templateRepository;
    private final BusinessService businessService;
    private final ServiceDefinitionService serviceDefinitionService;

    public PackageTemplateService(PackageTemplateRepository templateRepository,
                                   BusinessService businessService,
                                   ServiceDefinitionService serviceDefinitionService) {
        this.templateRepository = templateRepository;
        this.businessService = businessService;
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @Transactional
    public PackageTemplate create(CreatePackageTemplateRequest request) {
        businessService.findByIdOrThrow(request.businessId());
        serviceDefinitionService.findByIdOrThrow(request.serviceId());
        PackageTemplate template = PackageTemplateMapper.toEntity(request);
        return templateRepository.save(template);
    }

    @Transactional
    public PackageTemplate update(Long id, UpdatePackageTemplateRequest request) {
        PackageTemplate template = findByIdOrThrow(id);
        template.update(
            request.name(),
            request.totalSessions(),
            request.price()
        );
        return templateRepository.save(template);
    }

    @Transactional
    public void deactivate(Long id) {
        PackageTemplate template = findByIdOrThrow(id);
        template.deactivate();
        templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<PackageTemplate> listByBusiness(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return templateRepository.findByBusinessIdAndActiveTrue(businessId);
    }

    @Transactional(readOnly = true)
    public PackageTemplate findByIdOrThrow(Long id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Paket şablonu bulunamadı: " + id));
    }
}
