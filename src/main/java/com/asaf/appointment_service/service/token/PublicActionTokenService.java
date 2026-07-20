package com.asaf.appointment_service.service.token;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.exception.token.ActionTokenExpiredException;
import com.asaf.appointment_service.repository.token.PublicActionTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicActionTokenService {

    private static final int RESCHEDULE_TOKEN_VALID_HOURS = 48;

    private final PublicActionTokenRepository repository;

    public PublicActionTokenService(PublicActionTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PublicActionToken issueRescheduleToken(Long businessId, Long appointmentId) {
        PublicActionToken token = PublicActionToken.builder()
            .businessId(businessId)
            .appointmentId(appointmentId)
            .type(PublicActionTokenType.RESCHEDULE)
            .token(PublicActionToken.generateToken())
            .expiresAt(Instant.now().plus(RESCHEDULE_TOKEN_VALID_HOURS, ChronoUnit.HOURS))
            .build();
        return repository.save(token);
    }

    @Transactional(readOnly = true)
    public PublicActionToken validate(String token, PublicActionTokenType expectedType) {
        PublicActionToken entity = repository.findByToken(token)
            .filter(t -> t.getType() == expectedType)
            .orElseThrow(() -> new EntityNotFoundException("Bağlantı", token));
        if (!entity.isValid()) {
            throw new ActionTokenExpiredException();
        }
        return entity;
    }

    @Transactional
    public void consume(PublicActionToken token) {
        token.markAsUsed();
        repository.save(token);
    }
}
