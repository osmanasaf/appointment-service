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
    private static final int WAITLIST_OFFER_TOKEN_VALID_HOURS = 2;
    private static final int REVIEW_TOKEN_VALID_HOURS = 24 * 30;

    private final PublicActionTokenRepository repository;

    public PublicActionTokenService(PublicActionTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PublicActionToken issueRescheduleToken(Long businessId, Long appointmentId) {
        return issue(businessId, appointmentId, PublicActionTokenType.RESCHEDULE, RESCHEDULE_TOKEN_VALID_HOURS);
    }

    @Transactional
    public PublicActionToken issueWaitlistOfferToken(Long businessId, Long waitlistEntryId) {
        return issue(businessId, waitlistEntryId, PublicActionTokenType.WAITLIST_OFFER,
            WAITLIST_OFFER_TOKEN_VALID_HOURS);
    }

    @Transactional
    public PublicActionToken issueReviewToken(Long businessId, Long appointmentId) {
        return issue(businessId, appointmentId, PublicActionTokenType.REVIEW, REVIEW_TOKEN_VALID_HOURS);
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

    private PublicActionToken issue(Long businessId, Long subjectId, PublicActionTokenType type, int validHours) {
        PublicActionToken token = PublicActionToken.builder()
            .businessId(businessId)
            .subjectId(subjectId)
            .type(type)
            .token(PublicActionToken.generateToken())
            .expiresAt(Instant.now().plus(validHours, ChronoUnit.HOURS))
            .build();
        return repository.save(token);
    }
}
