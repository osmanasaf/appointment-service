package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.common.constants.PackageErrorMessages;
import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.dto.customer.CreatePackageRequest;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.service.PackageTemplate;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.service.service.PackageTemplateService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    private final CustomerService customerService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final PackageTemplateService packageTemplateService;
    private final PackageSessionService packageSessionService;
    private final CustomerPackageRepository packageRepository;

    public PackageService(CustomerService customerService,
                          ServiceDefinitionService serviceDefinitionService,
                          PackageTemplateService packageTemplateService,
                          PackageSessionService packageSessionService,
                          CustomerPackageRepository packageRepository) {
        this.customerService = customerService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.packageTemplateService = packageTemplateService;
        this.packageSessionService = packageSessionService;
        this.packageRepository = packageRepository;
    }

    @Transactional
    public CustomerPackage createPackage(CreatePackageRequest request) {
        Customer customer = customerService.findByIdOrThrow(request.customerId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(request.serviceId());

        if (!service.isPackageEligible()) {
            throw new ValidationException("PACKAGE_NOT_ELIGIBLE", PackageErrorMessages.PACKAGE_NOT_ELIGIBLE);
        }

        CustomerPackage customerPackage = CustomerPackage.builder()
            .customerId(customer.getId())
            .serviceId(service.getId())
            .businessId(customer.getBusinessId())
            .name(request.name())
            .sessions(request.totalSessions())
            .expiresAt(request.expiresAt())
            .build();

        CustomerPackage saved = packageRepository.save(customerPackage);
        log.info("Paket oluşturuldu: packageId={}, customerId={}, sessions={}", 
            saved.getId(), request.customerId(), request.totalSessions());
        return saved;
    }

    @Transactional
    public CustomerPackage createFromTemplate(Long customerId, Long templateId) {
        Customer customer = customerService.findByIdOrThrow(customerId);
        PackageTemplate template = packageTemplateService.findByIdOrThrow(templateId);
        if (!template.getBusinessId().equals(customer.getBusinessId())) {
            throw new ValidationException("TEMPLATE_WRONG_BUSINESS", PackageErrorMessages.TEMPLATE_WRONG_BUSINESS);
        }
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(template.getServiceId());
        if (!service.isPackageEligible()) {
            throw new ValidationException("PACKAGE_NOT_ELIGIBLE", PackageErrorMessages.PACKAGE_NOT_ELIGIBLE);
        }
        CustomerPackage customerPackage = CustomerPackage.builder()
            .customerId(customer.getId())
            .serviceId(template.getServiceId())
            .businessId(customer.getBusinessId())
            .name(template.getName())
            .sessions(template.getTotalSessions())
            .templateId(template.getId())
            .build();
        CustomerPackage saved = packageRepository.save(customerPackage);
        packageSessionService.createSessionsForPackage(saved);
        log.info("Şablondan paket oluşturuldu: packageId={}, customerId={}, templateId={}", 
            saved.getId(), customerId, templateId);
        return saved;
    }

    @Transactional
    public CustomerPackage deductSession(Long packageId) {
        CustomerPackage customerPackage = findByIdOrThrow(packageId);
        
        if (customerPackage.isExpired()) {
            throw new BusinessException("PACKAGE_EXPIRED", PackageErrorMessages.PACKAGE_EXPIRED);
        }
        
        if (!customerPackage.hasRemainingSessions()) {
            throw new BusinessException("NO_REMAINING_SESSIONS", PackageErrorMessages.NO_REMAINING_SESSIONS);
        }

        customerPackage.deductSession();
        CustomerPackage saved = packageRepository.save(customerPackage);
        
        log.info("Seans düşüldü: packageId={}, remaining={}", packageId, saved.getRemainingSessions());
        
        if (saved.isLowOnSessions()) {
            log.warn("Paket düşük seans: packageId={}, remaining={}", packageId, saved.getRemainingSessions());
        }
        
        return saved;
    }

    @Transactional
    public CustomerPackage addSession(Long packageId, int sessions) {
        CustomerPackage customerPackage = findByIdOrThrow(packageId);
        customerPackage.addSessions(sessions);
        CustomerPackage saved = packageRepository.save(customerPackage);
        log.info("Seans eklendi: packageId={}, added={}, total={}", 
            packageId, sessions, saved.getRemainingSessions());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CustomerPackage> findActiveByCustomerId(Long customerId) {
        customerService.findByIdOrThrow(customerId);
        return packageRepository.findByCustomerIdAndActiveTrue(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerPackage> findAllByCustomerId(Long customerId) {
        customerService.findByIdOrThrow(customerId);
        return packageRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * İşletmeye ait şablon için tüm müşteri paketleri (aktif/pasif, istatistik ve filtreler için).
     */
    @Transactional(readOnly = true)
    public List<CustomerPackage> findByBusinessIdAndTemplateId(Long businessId, Long templateId) {
        PackageTemplate template = packageTemplateService.findByIdOrThrow(templateId);
        if (!template.getBusinessId().equals(businessId)) {
            throw new ValidationException("TEMPLATE_WRONG_BUSINESS", PackageErrorMessages.TEMPLATE_WRONG_BUSINESS);
        }
        return packageRepository.findByBusinessIdAndTemplateIdOrderByCreatedAtDesc(businessId, templateId);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerPackage> findActivePackageForService(Long customerId, Long serviceId) {
        return packageRepository.findByCustomerIdAndServiceIdAndActiveTrue(customerId, serviceId)
            .stream()
            .filter(p -> !p.isExpired() && p.hasRemainingSessions())
            .findFirst();
    }

    @Transactional
    public void deductSessionForAppointment(Long customerId, Long serviceId) {
        Optional<CustomerPackage> activePackage = findActivePackageForService(customerId, serviceId);
        
        if (activePackage.isPresent()) {
            deductSession(activePackage.get().getId());
        }
    }

    @Transactional
    public void expireOverduePackages() {
        List<CustomerPackage> overduePackages = packageRepository.findByActiveTrueAndExpiresAtBefore(
            java.time.LocalDateTime.now());
        
        for (CustomerPackage pkg : overduePackages) {
            pkg.markAsExpired();
            packageRepository.save(pkg);
            log.info("Paket süresi doldu: packageId={}", pkg.getId());
        }
    }

    public CustomerPackage findByIdOrThrow(Long id) {
        return packageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(PackageErrorMessages.PACKAGE_NOT_FOUND + id));
    }

    @Transactional
    public void assignSessionToAppointment(Long sessionId, Long appointmentId, java.time.LocalDateTime scheduledAt) {
        packageSessionService.assignAppointment(sessionId, appointmentId, scheduledAt);
    }

    @Transactional
    public void unassignSessionFromAppointment(Long sessionId) {
        packageSessionService.unassignAppointment(sessionId);
    }
}
