package com.asaf.appointment_service.repository.customer;

import com.asaf.appointment_service.entity.customer.PackageSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageSessionRepository extends JpaRepository<PackageSession, Long> {

    List<PackageSession> findByCustomerPackageIdOrderBySessionNumberAsc(Long customerPackageId);

    Optional<PackageSession> findByAppointmentId(Long appointmentId);
}
