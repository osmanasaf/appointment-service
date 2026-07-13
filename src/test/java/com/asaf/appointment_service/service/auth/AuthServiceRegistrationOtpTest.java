package com.asaf.appointment_service.service.auth;

import com.asaf.appointment_service.dto.auth.RegisterRequest;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeInviteService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.NicheServiceTemplateService;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationOtpTest {

    @Mock private UserRepository userRepository;
    @Mock private BusinessService businessService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private SubscriptionService subscriptionService;
    @Mock private com.asaf.appointment_service.integration.email.EmailClient emailClient;
    @Mock private WhatsAppClientInterface whatsAppClient;
    @Mock private NicheServiceTemplateService nicheServiceTemplateService;
    @Mock private EmployeeInviteService employeeInviteService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSendOtpViaWhatsApp_whenNewUser() {
        RegisterRequest request = new RegisterRequest(
            "owner@test.com",
            "Password1",
            "Owner Name",
            "5551234567",
            "Test Business",
            BusinessCategory.OTHER
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        Business business = Business.builder().id(10L).name("Test Business").build();
        when(businessService.createBusiness(any())).thenReturn(business);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(whatsAppClient.sendOtp(eq("5551234567"), any()))
            .thenReturn(WhatsAppMessageResponse.success("msg-1"));

        authService.register(request);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppClient).sendOtp(eq("5551234567"), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).matches("\\d{6}");
    }
}
