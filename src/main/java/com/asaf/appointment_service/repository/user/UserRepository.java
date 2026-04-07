package com.asaf.appointment_service.repository.user;

import com.asaf.appointment_service.entity.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByBusinessId(Long businessId);

    Optional<User> findByEmployeeId(Long employeeId);
}
