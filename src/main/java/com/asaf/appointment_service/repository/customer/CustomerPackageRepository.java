package com.asaf.appointment_service.repository.customer;

import com.asaf.appointment_service.entity.customer.CustomerPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPackageRepository extends JpaRepository<CustomerPackage, Long> {

    List<CustomerPackage> findByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerPackage> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerPackage> findByCustomerIdAndServiceIdAndActiveTrue(Long customerId, Long serviceId);

    List<CustomerPackage> findByBusinessIdAndActiveTrue(Long businessId);

    List<CustomerPackage> findByBusinessIdAndTemplateIdOrderByCreatedAtDesc(Long businessId, Long templateId);

    List<CustomerPackage> findByActiveTrueAndExpiresAtBefore(java.time.LocalDateTime dateTime);
}
