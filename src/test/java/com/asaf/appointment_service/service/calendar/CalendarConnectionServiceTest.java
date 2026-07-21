package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.config.GoogleCalendarProperties;
import com.asaf.appointment_service.integration.google.dto.GoogleTokenResponse;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarConnectionServiceTest {

    private static final Long BUSINESS_ID = 1L;
    private static final Long EMPLOYEE_ID = 2L;

    @Mock
    private CalendarConnectionRepository repository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private GoogleCalendarClient googleCalendarClient;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PublicActionTokenService tokenService;

    private CalendarConnectionService connectionService;
    private GoogleCalendarProperties properties;

    private void setUp() {
        properties = new GoogleCalendarProperties(true, "client-id", "client-secret", "https://api.example.com/callback");
        connectionService = new CalendarConnectionService(
            repository, employeeService, googleCalendarClient, properties, encryptionService, subscriptionService, tokenService);
        lenient().when(employeeService.findByIdOrThrow(EMPLOYEE_ID))
            .thenReturn(Employee.builder().id(EMPLOYEE_ID).businessId(BUSINESS_ID).name("Ayşe").build());
    }

    private PublicActionToken stateToken() {
        PublicActionToken token = PublicActionToken.builder()
            .businessId(BUSINESS_ID).subjectId(EMPLOYEE_ID).type(PublicActionTokenType.GOOGLE_OAUTH_STATE)
            .token("state-token-value").expiresAt(Instant.now().plusSeconds(900)).build();
        return token;
    }

    @Test
    void should_throwPlanFeatureNotAvailable_when_calendarSyncDisabled() {
        setUp();
        doThrow(new PlanFeatureNotAvailableException(PlanFeature.CALENDAR_SYNC))
            .when(subscriptionService).requireFeature(BUSINESS_ID, PlanFeature.CALENDAR_SYNC);

        assertThatThrownBy(() -> connectionService.buildConnectUrl(BUSINESS_ID, EMPLOYEE_ID))
            .isInstanceOf(PlanFeatureNotAvailableException.class);
        verify(tokenService, never()).issueGoogleOauthStateToken(any(), any());
    }

    @Test
    void should_throwBusinessException_when_employeeNotInBusiness() {
        setUp();
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID))
            .thenReturn(Employee.builder().id(EMPLOYEE_ID).businessId(999L).name("Başka").build());

        assertThatThrownBy(() -> connectionService.buildConnectUrl(BUSINESS_ID, EMPLOYEE_ID))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_returnAuthorizationUrl_usingServerIssuedStateToken_when_buildConnectUrlSucceeds() {
        setUp();
        PublicActionToken issued = stateToken();
        when(tokenService.issueGoogleOauthStateToken(BUSINESS_ID, EMPLOYEE_ID)).thenReturn(issued);
        when(googleCalendarClient.buildAuthorizationUrl("state-token-value", properties.redirectUri()))
            .thenReturn("https://accounts.google.com/auth?state=state-token-value");

        String url = connectionService.buildConnectUrl(BUSINESS_ID, EMPLOYEE_ID);

        assertThat(url).isEqualTo("https://accounts.google.com/auth?state=state-token-value");
        verify(subscriptionService).requireFeature(BUSINESS_ID, PlanFeature.CALENDAR_SYNC);
    }

    @Test
    void should_saveNewConnection_when_handleCallbackSucceeds() {
        setUp();
        PublicActionToken issued = stateToken();
        when(tokenService.validate("state-token-value", PublicActionTokenType.GOOGLE_OAUTH_STATE)).thenReturn(issued);
        when(googleCalendarClient.exchangeAuthorizationCode("auth-code", properties.redirectUri()))
            .thenReturn(GoogleTokenResponse.success("access-tok", "refresh-tok", 3600));
        when(encryptionService.encrypt("refresh-tok")).thenReturn("enc-refresh");
        when(encryptionService.encrypt("access-tok")).thenReturn("enc-access");
        when(repository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        connectionService.handleCallback("state-token-value", "auth-code");

        var captor = org.mockito.ArgumentCaptor.forClass(CalendarConnection.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(captor.getValue().getRefreshTokenEncrypted()).isEqualTo("enc-refresh");
        assertThat(captor.getValue().getStatus()).isEqualTo(CalendarConnectionStatus.CONNECTED);
        verify(tokenService).consume(issued);
    }

    @Test
    void should_throwBusinessException_when_handleCallbackTokenExchangeFails() {
        setUp();
        PublicActionToken issued = stateToken();
        when(tokenService.validate("state-token-value", PublicActionTokenType.GOOGLE_OAUTH_STATE)).thenReturn(issued);
        when(googleCalendarClient.exchangeAuthorizationCode("bad-code", properties.redirectUri()))
            .thenReturn(GoogleTokenResponse.failure("invalid_grant"));

        assertThatThrownBy(() -> connectionService.handleCallback("state-token-value", "bad-code"))
            .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
        verify(tokenService, never()).consume(any());
    }

    @Test
    void should_propagate_when_stateTokenInvalidOrExpiredOrAlreadyUsed() {
        setUp();
        when(tokenService.validate("bad-state", PublicActionTokenType.GOOGLE_OAUTH_STATE))
            .thenThrow(new BusinessException("TOKEN_EXPIRED", "Bağlantı süresi doldu"));

        assertThatThrownBy(() -> connectionService.handleCallback("bad-state", "code"))
            .isInstanceOf(BusinessException.class);
        verify(googleCalendarClient, never()).exchangeAuthorizationCode(any(), any());
    }

    @Test
    void should_returnDecryptedAccessToken_when_currentTokenStillValid() {
        setUp();
        CalendarConnection connection = connectionWithValidAccessToken();
        when(encryptionService.decrypt("enc-access")).thenReturn("plain-access");

        String token = connectionService.resolveAccessToken(connection);

        assertThat(token).isEqualTo("plain-access");
        verify(googleCalendarClient, never()).refreshAccessToken(any());
    }

    @Test
    void should_refreshAndSave_when_accessTokenExpired() {
        setUp();
        CalendarConnection connection = connectionWithExpiredAccessToken();
        when(encryptionService.decrypt("enc-refresh")).thenReturn("plain-refresh");
        when(googleCalendarClient.refreshAccessToken("plain-refresh"))
            .thenReturn(GoogleTokenResponse.success("new-access", null, 3600));
        when(encryptionService.encrypt("new-access")).thenReturn("enc-new-access");

        String token = connectionService.resolveAccessToken(connection);

        assertThat(token).isEqualTo("new-access");
        verify(repository).save(connection);
        assertThat(connection.getStatus()).isEqualTo(CalendarConnectionStatus.CONNECTED);
    }

    @Test
    void should_markBroken_when_refreshFails() {
        setUp();
        CalendarConnection connection = connectionWithExpiredAccessToken();
        when(encryptionService.decrypt("enc-refresh")).thenReturn("plain-refresh");
        when(googleCalendarClient.refreshAccessToken("plain-refresh"))
            .thenReturn(GoogleTokenResponse.failure("invalid_grant"));

        String token = connectionService.resolveAccessToken(connection);

        assertThat(token).isNull();
        assertThat(connection.getStatus()).isEqualTo(CalendarConnectionStatus.BROKEN);
        verify(repository).save(connection);
    }

    @Test
    void should_deleteConnection_when_disconnectCalled() {
        setUp();
        connectionService.disconnect(BUSINESS_ID, EMPLOYEE_ID);

        verify(repository).deleteByEmployeeId(EMPLOYEE_ID);
    }

    private CalendarConnection connectionWithValidAccessToken() {
        CalendarConnection connection = CalendarConnection.builder()
            .businessId(BUSINESS_ID).employeeId(EMPLOYEE_ID).refreshTokenEncrypted("enc-refresh").build();
        ReflectionTestUtils.setField(connection, "accessTokenEncrypted", "enc-access");
        ReflectionTestUtils.setField(connection, "accessTokenExpiresAt", LocalDateTime.now().plusHours(1));
        return connection;
    }

    private CalendarConnection connectionWithExpiredAccessToken() {
        CalendarConnection connection = CalendarConnection.builder()
            .businessId(BUSINESS_ID).employeeId(EMPLOYEE_ID).refreshTokenEncrypted("enc-refresh").build();
        ReflectionTestUtils.setField(connection, "accessTokenEncrypted", "enc-access");
        ReflectionTestUtils.setField(connection, "accessTokenExpiresAt", LocalDateTime.now().minusMinutes(5));
        return connection;
    }
}
