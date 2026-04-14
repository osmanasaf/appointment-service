package com.asaf.appointment_service.repository.user;

import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    long countByRole(UserRole role);

    List<User> findByBusinessId(Long businessId);

    long countByCreatedAtAfter(LocalDateTime after);
}
