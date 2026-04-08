package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.dto.employee.AssignCapabilityRequest;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.employee.SkillLevel;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.employee.EmployeeCapabilityNotFoundException;
import com.asaf.appointment_service.repository.employee.EmployeeCapabilityRepository;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeCapabilityService {

    private final EmployeeCapabilityRepository capabilityRepository;
    private final EmployeeService employeeService;
    private final ServiceDefinitionService serviceDefinitionService;

    public EmployeeCapabilityService(EmployeeCapabilityRepository capabilityRepository,
                                     EmployeeService employeeService,
                                     ServiceDefinitionService serviceDefinitionService) {
        this.capabilityRepository = capabilityRepository;
        this.employeeService = employeeService;
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @Transactional
    public EmployeeServiceCapability assignCapability(Long employeeId, AssignCapabilityRequest request) {
        employeeService.findByIdOrThrow(employeeId);
        serviceDefinitionService.findByIdOrThrow(request.serviceId());

        return capabilityRepository.findByEmployeeIdAndServiceId(employeeId, request.serviceId())
            .map(existing -> updateAndActivate(existing, request))
            .orElseGet(() -> createNew(employeeId, request));
    }

    @Transactional
    public void removeCapability(Long employeeId, Long serviceId) {
        EmployeeServiceCapability capability = capabilityRepository
            .findByEmployeeIdAndServiceId(employeeId, serviceId)
            .orElseThrow(() -> {
                String empName = employeeService.findByIdOrThrow(employeeId).getName();
                String svcName = serviceDefinitionService.findByIdOrThrow(serviceId).getName();
                return new EmployeeCapabilityNotFoundException(empName, svcName);
            });
        capability.deactivate();
        capabilityRepository.save(capability);
    }

    @Transactional(readOnly = true)
    public List<EmployeeServiceCapability> getByEmployee(Long employeeId) {
        employeeService.findByIdOrThrow(employeeId);
        return capabilityRepository.findByEmployeeIdAndActiveTrue(employeeId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeServiceCapability> getByService(Long serviceId) {
        serviceDefinitionService.findByIdOrThrow(serviceId);
        return capabilityRepository.findByServiceIdAndActiveTrue(serviceId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeServiceCapability> getActiveByServiceAndBusiness(Long serviceId, Long businessId) {
        return capabilityRepository.findActiveByServiceIdAndBusinessId(serviceId, businessId);
    }

    @Transactional(readOnly = true)
    public int effectiveDuration(Long employeeId, Long serviceId, int serviceDefaultDuration) {
        return capabilityRepository.findByEmployeeIdAndServiceId(employeeId, serviceId)
            .filter(EmployeeServiceCapability::isActive)
            .map(c -> c.effectiveDuration(serviceDefaultDuration))
            .orElse(serviceDefaultDuration);
    }

    @Transactional(readOnly = true)
    public BigDecimal effectivePrice(Long employeeId, Long serviceId, BigDecimal serviceDefaultPrice) {
        return capabilityRepository.findByEmployeeIdAndServiceId(employeeId, serviceId)
            .filter(EmployeeServiceCapability::isActive)
            .map(c -> c.effectivePrice(serviceDefaultPrice))
            .orElse(serviceDefaultPrice);
    }

    public void validateCapability(Long employeeId, Long serviceId) {
        if (!capabilityRepository.existsByEmployeeIdAndServiceIdAndActiveTrue(employeeId, serviceId)) {
            String empName = employeeService.findByIdOrThrow(employeeId).getName();
            String svcName = serviceDefinitionService.findByIdOrThrow(serviceId).getName();
            throw new EmployeeCapabilityNotFoundException(empName, svcName);
        }
    }

    public boolean hasCapability(Long employeeId, Long serviceId) {
        return capabilityRepository.existsByEmployeeIdAndServiceIdAndActiveTrue(employeeId, serviceId);
    }

    private EmployeeServiceCapability updateAndActivate(EmployeeServiceCapability existing,
                                                        AssignCapabilityRequest request) {
        existing.activate();
        existing.updateDetails(
            request.customDurationMinutes(),
            request.customPrice(),
            request.skillLevel()
        );
        return capabilityRepository.save(existing);
    }

    private EmployeeServiceCapability createNew(Long employeeId, AssignCapabilityRequest request) {
        SkillLevel level = request.skillLevel() != null ? request.skillLevel() : SkillLevel.INTERMEDIATE;
        EmployeeServiceCapability capability = EmployeeServiceCapability.builder()
            .employeeId(employeeId)
            .serviceId(request.serviceId())
            .customDurationMinutes(request.customDurationMinutes())
            .customPrice(request.customPrice())
            .skillLevel(level)
            .build();
        return capabilityRepository.save(capability);
    }

    private ServiceDefinition resolveService(Long serviceId) {
        return serviceDefinitionService.findByIdOrThrow(serviceId);
    }
}
