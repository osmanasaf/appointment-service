package com.asaf.appointment_service.entity.token;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicActionTokenTest {

    private PublicActionToken tokenWithExpiry(Instant expiresAt) {
        return PublicActionToken.builder()
            .businessId(1L)
            .appointmentId(10L)
            .type(PublicActionTokenType.RESCHEDULE)
            .token("abc123")
            .expiresAt(expiresAt)
            .build();
    }

    @Test
    void should_beValid_when_pendingAndNotExpired() {
        PublicActionToken token = tokenWithExpiry(Instant.now().plusSeconds(3600));

        assertThat(token.isExpired()).isFalse();
        assertThat(token.isValid()).isTrue();
    }

    @Test
    void should_beInvalid_when_expired() {
        PublicActionToken token = tokenWithExpiry(Instant.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void should_beInvalid_when_markedAsUsed() {
        PublicActionToken token = tokenWithExpiry(Instant.now().plusSeconds(3600));

        token.markAsUsed();

        assertThat(token.getStatus()).isEqualTo(PublicActionTokenStatus.USED);
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void should_generateUniqueOpaqueTokens_when_generateTokenCalled() {
        String token1 = PublicActionToken.generateToken();
        String token2 = PublicActionToken.generateToken();

        assertThat(token1).doesNotContain("-").hasSize(32);
        assertThat(token1).isNotEqualTo(token2);
    }
}
