package com.asaf.appointment_service.repository.customer;

import com.asaf.appointment_service.entity.customer.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Customer> findByBusinessIdAndPhoneNumber(Long businessId, String phoneNumber);

    boolean existsByBusinessIdAndPhoneNumber(Long businessId, String phoneNumber);

    List<Customer> findByBusinessIdAndBlacklistedTrue(Long businessId);

    List<Customer> findByBusinessIdAndNoShowCountGreaterThanEqual(Long businessId, int count);
}
