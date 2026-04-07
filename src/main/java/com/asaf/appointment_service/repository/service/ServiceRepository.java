package com.asaf.appointment_service.repository.service;

import com.asaf.appointment_service.entity.service.ServiceDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceDefinition, Long> {

    List<ServiceDefinition> findByBusinessIdOrderByDisplayOrderAsc(Long businessId);

    List<ServiceDefinition> findByBusinessIdAndActiveTrue(Long businessId);

    List<ServiceDefinition> findByBusinessIdAndPackageEligibleTrue(Long businessId);

    boolean existsByBusinessIdAndName(Long businessId, String name);

    long countByBusinessId(Long businessId);
}
