package com.asaf.appointment_service.repository.employee;

import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByBusinessIdOrderByNameAsc(Long businessId);

    List<Employee> findByBusinessIdAndStatus(Long businessId, EmployeeStatus status);

    boolean existsByBusinessIdAndPhoneNumber(Long businessId, String phoneNumber);

    long countByBusinessId(Long businessId);
}
