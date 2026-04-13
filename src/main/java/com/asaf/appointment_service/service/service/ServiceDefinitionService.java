package com.asaf.appointment_service.service.service;

import com.asaf.appointment_service.common.service.BaseService;
import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.dto.service.UpdateServiceRequest;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.service.ServiceNotFoundException;
import com.asaf.appointment_service.mapper.service.ServiceMapper;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceDefinitionService extends BaseService<ServiceDefinition, Long, ServiceRepository> {

    private final BusinessService businessService;

    public ServiceDefinitionService(ServiceRepository repository, BusinessService businessService) {
        super(repository, "Hizmet");
        this.businessService = businessService;
    }

    @Transactional
    public ServiceDefinition createService(Long businessId, CreateServiceRequest request) {
        businessService.findByIdOrThrow(businessId);
        ServiceDefinition service = ServiceMapper.toEntity(businessId, request);
        return repository.save(service);
    }

    @Transactional
    public ServiceDefinition updateService(Long id, UpdateServiceRequest request) {
        ServiceDefinition service = findByIdOrThrow(id);
        service.updateDetails(
            request.name(),
            request.description(),
            request.durationMinutes(),
            request.price()
        );
        service.updateBuffers(request.bufferBeforeMinutes(), request.bufferAfterMinutes());
        service.updateFlags(
            request.requiresConfirmation(),
            request.sameDayBookingAllowed(),
            request.packageEligible()
        );
        return repository.save(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceDefinition> findByBusinessId(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdOrderByDisplayOrderAsc(businessId);
    }

    @Transactional(readOnly = true)
    public List<ServiceDefinition> findActiveByBusinessId(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndActiveTrue(businessId);
    }

    @Transactional(readOnly = true)
    public List<ServiceDefinition> findPackageEligibleByBusinessId(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return repository.findByBusinessIdAndPackageEligibleTrue(businessId);
    }

    @Transactional(readOnly = true)
    public List<ServiceDefinition> findByBusinessIdFiltered(Long businessId, boolean activeOnly, Boolean packageEligible) {
        businessService.findByIdOrThrow(businessId);
        if (Boolean.TRUE.equals(packageEligible)) {
            List<ServiceDefinition> services = repository.findByBusinessIdAndPackageEligibleTrue(businessId);
            if (activeOnly) {
                return services.stream().filter(ServiceDefinition::isActive).toList();
            }
            return services;
        } else if (activeOnly) {
            return repository.findByBusinessIdAndActiveTrue(businessId);
        } else {
            return repository.findByBusinessIdOrderByDisplayOrderAsc(businessId);
        }
    }

    @Transactional
    public ServiceDefinition activateService(Long id) {
        ServiceDefinition service = findByIdOrThrow(id);
        service.activate();
        return repository.save(service);
    }

    @Transactional
    public ServiceDefinition deactivateService(Long id) {
        ServiceDefinition service = findByIdOrThrow(id);
        service.deactivate();
        return repository.save(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceDefinition> findAllByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public ServiceDefinition findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ServiceNotFoundException(id));
    }
}
