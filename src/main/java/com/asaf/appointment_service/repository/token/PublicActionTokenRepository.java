package com.asaf.appointment_service.repository.token;

import com.asaf.appointment_service.entity.token.PublicActionToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicActionTokenRepository extends JpaRepository<PublicActionToken, Long> {

    Optional<PublicActionToken> findByToken(String token);
}
