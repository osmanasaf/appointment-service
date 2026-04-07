package com.asaf.appointment_service.repository.business;

import com.asaf.appointment_service.entity.business.Business;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByPhoneNumber(String phoneNumber);
}
