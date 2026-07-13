package com.asaf.appointment_service.entity.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpCredentialTest {

    @Test
    void should_generateSixDigitCode_when_generateCalled() {
        OtpCredential credential = new OtpCredential();

        String code = credential.generate(5);

        assertThat(code).matches("\\d{6}");
        assertThat(credential.hasActiveCode()).isTrue();
    }

    @Test
    void should_returnTrue_when_verifyWithCorrectCode() {
        OtpCredential credential = new OtpCredential();
        String code = credential.generate(5);

        assertThat(credential.verify(code)).isTrue();
        assertThat(credential.hasActiveCode()).isFalse();
    }

    @Test
    void should_returnFalse_when_verifyWithWrongCode() {
        OtpCredential credential = new OtpCredential();
        credential.generate(5);

        assertThat(credential.verify("000000")).isFalse();
        assertThat(credential.hasActiveCode()).isTrue();
    }

    @Test
    void should_blockAfterMaxAttempts_when_verifyFailsRepeatedly() {
        OtpCredential credential = new OtpCredential();
        credential.generate(5);

        for (int i = 0; i < 5; i++) {
            credential.verify("000000");
        }

        assertThat(credential.isBlocked()).isTrue();
        assertThatThrownBy(() -> credential.generate(5))
            .isInstanceOf(IllegalStateException.class);
    }
}
