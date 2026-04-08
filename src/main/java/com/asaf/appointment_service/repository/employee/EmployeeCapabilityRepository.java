package com.asaf.appointment_service.repository.employee;

import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeCapabilityRepository extends JpaRepository<EmployeeServiceCapability, Long> {

    List<EmployeeServiceCapability> findByEmployeeIdAndActiveTrue(Long employeeId);

    List<EmployeeServiceCapability> findByServiceIdAndActiveTrue(Long serviceId);

    Optional<EmployeeServiceCapability> findByEmployeeIdAndServiceId(Long employeeId, Long serviceId);

    boolean existsByEmployeeIdAndServiceIdAndActiveTrue(Long employeeId, Long serviceId);

    @Query("""
        SELECT c FROM EmployeeServiceCapability c
        WHERE c.serviceId = :serviceId
          AND c.active = true
          AND c.employeeId IN (
              SELECT e.id FROM Employee e
              WHERE e.businessId = :businessId AND e.status = 'ACTIVE'
          )
        """)
    List<EmployeeServiceCapability> findActiveByServiceIdAndBusinessId(
        @Param("serviceId") Long serviceId,
        @Param("businessId") Long businessId
    );
}
