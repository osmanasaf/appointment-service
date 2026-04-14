package com.asaf.appointment_service.repository.business;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Business> findByNameContainingIgnoreCase(String name, Pageable pageable);

    long countByStatus(BusinessStatus status);

    long countByCreatedAtAfter(LocalDateTime after);
}
