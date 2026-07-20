package com.asaf.appointment_service.service.token;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenStatus;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.exception.token.ActionTokenExpiredException;
import com.asaf.appointment_service.repository.token.PublicActionTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicActionTokenServiceTest {

    @Mock
    private PublicActionTokenRepository repository;

    @InjectMocks
    private PublicActionTokenService tokenService;

    @Test
    void should_saveTokenWithRescheduleTypeAnd48HourExpiry_when_issueRescheduleTokenCalled() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicActionToken result = tokenService.issueRescheduleToken(1L, 100L);

        ArgumentCaptor<PublicActionToken> captor = ArgumentCaptor.forClass(PublicActionToken.class);
        verify(repository).save(captor.capture());
        PublicActionToken saved = captor.getValue();
        assertThat(saved.getBusinessId()).isEqualTo(1L);
        assertThat(saved.getSubjectId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo(PublicActionTokenType.RESCHEDULE);
        assertThat(saved.getStatus()).isEqualTo(PublicActionTokenStatus.PENDING);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(47, ChronoUnit.HOURS));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(49, ChronoUnit.HOURS));
        assertThat(result).isSameAs(saved);
    }

    @Test
    void should_saveTokenWithWaitlistOfferTypeAnd2HourExpiry_when_issueWaitlistOfferTokenCalled() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicActionToken result = tokenService.issueWaitlistOfferToken(1L, 200L);

        ArgumentCaptor<PublicActionToken> captor = ArgumentCaptor.forClass(PublicActionToken.class);
        verify(repository).save(captor.capture());
        PublicActionToken saved = captor.getValue();
        assertThat(saved.getSubjectId()).isEqualTo(200L);
        assertThat(saved.getType()).isEqualTo(PublicActionTokenType.WAITLIST_OFFER);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(1, ChronoUnit.HOURS));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(3, ChronoUnit.HOURS));
        assertThat(result).isSameAs(saved);
    }

    @Test
    void should_saveTokenWithReviewTypeAnd30DayExpiry_when_issueReviewTokenCalled() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicActionToken result = tokenService.issueReviewToken(1L, 300L);

        ArgumentCaptor<PublicActionToken> captor = ArgumentCaptor.forClass(PublicActionToken.class);
        verify(repository).save(captor.capture());
        PublicActionToken saved = captor.getValue();
        assertThat(saved.getSubjectId()).isEqualTo(300L);
        assertThat(saved.getType()).isEqualTo(PublicActionTokenType.REVIEW);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(31, ChronoUnit.DAYS));
        assertThat(result).isSameAs(saved);
    }

    @Test
    void should_reuseExistingValidToken_when_getOrIssueCalendarIcsTokenCalledAgain() {
        PublicActionToken existing = PublicActionToken.builder()
            .businessId(1L).subjectId(400L).type(PublicActionTokenType.CALENDAR_ICS)
            .token("existing-tok").expiresAt(Instant.now().plusSeconds(3600)).build();
        when(repository.findFirstBySubjectIdAndTypeOrderByCreatedAtDesc(400L, PublicActionTokenType.CALENDAR_ICS))
            .thenReturn(Optional.of(existing));

        PublicActionToken result = tokenService.getOrIssueCalendarIcsToken(1L, 400L);

        assertThat(result).isSameAs(existing);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void should_issueNewToken_when_getOrIssueCalendarIcsTokenCalledWithNoExistingToken() {
        when(repository.findFirstBySubjectIdAndTypeOrderByCreatedAtDesc(400L, PublicActionTokenType.CALENDAR_ICS))
            .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicActionToken result = tokenService.getOrIssueCalendarIcsToken(1L, 400L);

        assertThat(result.getType()).isEqualTo(PublicActionTokenType.CALENDAR_ICS);
        assertThat(result.getSubjectId()).isEqualTo(400L);
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plus(360, ChronoUnit.DAYS));
    }

    @Test
    void should_issueNewToken_when_existingTokenIsExpired() {
        PublicActionToken expired = PublicActionToken.builder()
            .businessId(1L).subjectId(400L).type(PublicActionTokenType.CALENDAR_ICS)
            .token("expired-tok").expiresAt(Instant.now().minusSeconds(1)).build();
        when(repository.findFirstBySubjectIdAndTypeOrderByCreatedAtDesc(400L, PublicActionTokenType.CALENDAR_ICS))
            .thenReturn(Optional.of(expired));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicActionToken result = tokenService.getOrIssueCalendarIcsToken(1L, 400L);

        assertThat(result).isNotSameAs(expired);
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void should_returnToken_when_validateCalledWithValidPendingToken() {
        PublicActionToken token = validToken();
        when(repository.findByToken("tok")).thenReturn(Optional.of(token));

        PublicActionToken result = tokenService.validate("tok", PublicActionTokenType.RESCHEDULE);

        assertThat(result).isSameAs(token);
    }

    @Test
    void should_throwEntityNotFound_when_tokenDoesNotExist() {
        when(repository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validate("missing", PublicActionTokenType.RESCHEDULE))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void should_throwActionTokenExpired_when_tokenAlreadyUsed() {
        PublicActionToken token = validToken();
        token.markAsUsed();
        when(repository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validate("tok", PublicActionTokenType.RESCHEDULE))
            .isInstanceOf(ActionTokenExpiredException.class);
    }

    @Test
    void should_throwActionTokenExpired_when_tokenExpired() {
        PublicActionToken token = PublicActionToken.builder()
            .businessId(1L).subjectId(100L).type(PublicActionTokenType.RESCHEDULE)
            .token("tok").expiresAt(Instant.now().minusSeconds(1)).build();
        when(repository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.validate("tok", PublicActionTokenType.RESCHEDULE))
            .isInstanceOf(ActionTokenExpiredException.class);
    }

    @Test
    void should_markTokenAsUsedAndSave_when_consumeCalled() {
        PublicActionToken token = validToken();

        tokenService.consume(token);

        assertThat(token.getStatus()).isEqualTo(PublicActionTokenStatus.USED);
        verify(repository).save(token);
    }

    private PublicActionToken validToken() {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(100L).type(PublicActionTokenType.RESCHEDULE)
            .token("tok").expiresAt(Instant.now().plusSeconds(3600)).build();
    }
}
