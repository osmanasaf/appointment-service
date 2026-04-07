package com.asaf.appointment_service.repository.service;

import com.asaf.appointment_service.entity.service.PackageTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageTemplateRepository extends JpaRepository<PackageTemplate, Long> {

    List<PackageTemplate> findByBusinessIdAndActiveTrue(Long businessId);

    List<PackageTemplate> findByBusinessIdOrderByName(Long businessId);

    boolean existsByBusinessIdAndServiceIdAndName(Long businessId, Long serviceId, String name);
}
