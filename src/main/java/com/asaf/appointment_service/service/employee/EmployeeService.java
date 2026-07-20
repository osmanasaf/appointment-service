package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.common.service.BusinessAwareService;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeStatus;
import com.asaf.appointment_service.exception.employee.EmployeeNotFoundException;
import com.asaf.appointment_service.mapper.employee.EmployeeMapper;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService extends BusinessAwareService<Employee, Long, EmployeeRepository> {

    public EmployeeService(EmployeeRepository repository, BusinessService businessService) {
        super(repository, "Çalışan", businessService);
    }

    @Transactional
    public Employee createEmployee(Long businessId, CreateEmployeeRequest request) {
        validateBusinessExists(businessId);
        Employee employee = EmployeeMapper.toEntity(businessId, request);
        return repository.save(employee);
    }

    @Transactional
    public Employee updateProfile(Long id, com.asaf.appointment_service.dto.employee.UpdateEmployeeProfileRequest request) {
        Employee employee = findByIdOrThrow(id);
        employee.updateFullProfile(
            request.name(),
            request.title(),
            request.email(),
            request.bio(),
            request.profileImageUrl(),
            request.experienceYears(),
            request.acceptsOnlineBooking()
        );
        return repository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByBusinessId(Long businessId) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdOrderByNameAsc(businessId);
    }

    @Transactional(readOnly = true)
    public Page<Employee> findByBusinessIdPaged(Long businessId, Pageable pageable) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdOrderByNameAsc(businessId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Employee> findActiveByBusinessId(Long businessId) {
        validateBusinessExists(businessId);
        return repository.findByBusinessIdAndStatus(businessId, EmployeeStatus.ACTIVE);
    }

    @Transactional
    public Employee activateEmployee(Long id) {
        Employee employee = findByIdOrThrow(id);
        employee.activate();
        return repository.save(employee);
    }

    @Transactional
    public Employee deactivateEmployee(Long id) {
        Employee employee = findByIdOrThrow(id);
        employee.deactivate();
        return repository.save(employee);
    }

    @Transactional
    public Employee updatePublicVisibility(Long id, boolean showOnPublicPage) {
        Employee employee = findByIdOrThrow(id);
        employee.updatePublicVisibility(showOnPublicPage);
        return repository.save(employee);
    }

    @Override
    public Employee findByIdOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
