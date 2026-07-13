package com.asaf.appointment_service.service.otp;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.entity.otp.PublicOtpVerification;
import com.asaf.appointment_service.exception.auth.InvalidOtpException;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.otp.PublicOtpVerificationRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicOtpServiceTest {

    @Mock
    private PublicOtpVerificationRepository repository;

    @Mock
    private WhatsAppClientInterface whatsAppClient;

    @InjectMocks
    private PublicOtpService publicOtpService;

    @Test
    void requestOtp_shouldSendWhatsAppAndPersistVerification() {
        when(repository.findByBusinessIdAndPhoneNormalized(1L, "905551234567"))
            .thenReturn(Optional.empty());
        when(repository.save(any(PublicOtpVerification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(whatsAppClient.sendOtp(eq("+905551234567"), any()))
            .thenReturn(WhatsAppMessageResponse.success("msg-1"));

        publicOtpService.requestOtp(1L, "+90", "5551234567");

        ArgumentCaptor<PublicOtpVerification> captor = ArgumentCaptor.forClass(PublicOtpVerification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPhoneNormalized()).isEqualTo("905551234567");
        verify(whatsAppClient).sendOtp(eq("+905551234567"), any());
    }

    @Test
    void requestOtp_shouldThrow_whenWhatsAppSendFails() {
        when(repository.findByBusinessIdAndPhoneNormalized(1L, "905551234567"))
            .thenReturn(Optional.empty());
        when(repository.save(any(PublicOtpVerification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(whatsAppClient.sendOtp(any(), any()))
            .thenReturn(WhatsAppMessageResponse.failure("send failed"));

        assertThatThrownBy(() -> publicOtpService.requestOtp(1L, "+90", "5551234567"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Doğrulama kodu gönderilemedi");
    }

    @Test
    void verifyAndConsume_shouldDeleteRecord_whenCodeValid() {
        PublicOtpVerification verification = new PublicOtpVerification(1L, "905551234567");
        String code = verification.getOtpCredential().generate(5);
        when(repository.findByBusinessIdAndPhoneNormalized(1L, "905551234567"))
            .thenReturn(Optional.of(verification));

        publicOtpService.verifyAndConsume(1L, "+90", "5551234567", code);

        verify(repository).delete(verification);
    }

    @Test
    void verifyAndConsume_shouldThrow_whenCodeInvalid() {
        PublicOtpVerification verification = new PublicOtpVerification(1L, "905551234567");
        verification.getOtpCredential().generate(5);
        when(repository.findByBusinessIdAndPhoneNormalized(1L, "905551234567"))
            .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> publicOtpService.verifyAndConsume(1L, "+90", "5551234567", "000000"))
            .isInstanceOf(InvalidOtpException.class);
    }
}
