package com.asaf.appointment_service.repository.customer;

import com.asaf.appointment_service.entity.customer.CustomerPackage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPackageRepository extends JpaRepository<CustomerPackage, Long> {

    List<CustomerPackage> findByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerPackage> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerPackage> findByCustomerIdAndServiceIdAndActiveTrue(Long customerId, Long serviceId);

    List<CustomerPackage> findByBusinessIdAndActiveTrue(Long businessId);

    List<CustomerPackage> findByBusinessIdAndTemplateIdOrderByCreatedAtDesc(Long businessId, Long templateId);

    @Query("""
        SELECT p FROM CustomerPackage p
        WHERE p.businessId = :businessId
          AND (:templateId IS NULL OR p.templateId = :templateId)
          AND (
            :status = 'all'
            OR (:status = 'active'
                AND p.active = true
                AND p.remainingSessions > 0
                AND (p.expiresAt IS NULL OR p.expiresAt > :now))
            OR (:status = 'low'
                AND p.active = true
                AND p.remainingSessions > 0
                AND p.remainingSessions <= 2
                AND (p.expiresAt IS NULL OR p.expiresAt > :now))
            OR (:status = 'expired'
                AND (p.active = false OR p.remainingSessions = 0 OR p.expiresAt <= :now))
          )
        """)
    Page<CustomerPackage> findBusinessPackages(
        @Param("businessId") Long businessId,
        @Param("status") String status,
        @Param("templateId") Long templateId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    List<CustomerPackage> findByActiveTrueAndExpiresAtBefore(java.time.LocalDateTime dateTime);
}
