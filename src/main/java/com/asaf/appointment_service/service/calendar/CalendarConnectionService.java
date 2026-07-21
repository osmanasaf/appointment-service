package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.config.GoogleCalendarProperties;
import com.asaf.appointment_service.integration.google.dto.GoogleTokenResponse;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışan bazlı Google Takvim bağlantısını yönetir: OAuth bağlama akışı, token
 * şifreleme/yenileme (StoredCard ile aynı EncryptionService deseni) ve bağlantı durumu.
 *
 * OAuth "state" parametresi olarak kendiliğinden anlam taşıyan (self-describing) bir değer
 * KULLANILMAZ — böyle bir değer sahtelenip başka bir işletmenin çalışanına, saldırganın
 * kendi Google hesabı bağlanacak şekilde kandırılabilir (CSRF/hesap karıştırma). Bunun yerine
 * PublicActionTokenService ile üretilen, sunucu tarafında doğrulanan, tek kullanımlık ve kısa
 * ömürlü bir token kullanılır (subjectId = employeeId); businessId callback'te İSTEMCİDEN değil
 * bu token üzerinden bulunan employee kaydından okunur.
 */
@Service
public class CalendarConnectionService {

    private static final Logger log = LoggerFactory.getLogger(CalendarConnectionService.class);

    private final CalendarConnectionRepository repository;
    private final EmployeeService employeeService;
    private final GoogleCalendarClient googleCalendarClient;
    private final GoogleCalendarProperties properties;
    private final EncryptionService encryptionService;
    private final SubscriptionService subscriptionService;
    private final PublicActionTokenService tokenService;

    public CalendarConnectionService(CalendarConnectionRepository repository,
                                      EmployeeService employeeService,
                                      GoogleCalendarClient googleCalendarClient,
                                      GoogleCalendarProperties properties,
                                      EncryptionService encryptionService,
                                      SubscriptionService subscriptionService,
                                      PublicActionTokenService tokenService) {
        this.repository = repository;
        this.employeeService = employeeService;
        this.googleCalendarClient = googleCalendarClient;
        this.properties = properties;
        this.encryptionService = encryptionService;
        this.subscriptionService = subscriptionService;
        this.tokenService = tokenService;
    }

    @Transactional
    public String buildConnectUrl(Long businessId, Long employeeId) {
        subscriptionService.requireFeature(businessId, PlanFeature.CALENDAR_SYNC);
        validateEmployeeInBusiness(businessId, employeeId);
        PublicActionToken stateToken = tokenService.issueGoogleOauthStateToken(businessId, employeeId);
        return googleCalendarClient.buildAuthorizationUrl(stateToken.getToken(), properties.redirectUri());
    }

    @Transactional
    public void handleCallback(String state, String code) {
        PublicActionToken stateToken = tokenService.validate(state, PublicActionTokenType.GOOGLE_OAUTH_STATE);
        Long employeeId = stateToken.getSubjectId();
        Employee employee = employeeService.findByIdOrThrow(employeeId);
        Long businessId = employee.getBusinessId();
        subscriptionService.requireFeature(businessId, PlanFeature.CALENDAR_SYNC);

        GoogleTokenResponse tokenResponse = googleCalendarClient.exchangeAuthorizationCode(code, properties.redirectUri());
        if (!tokenResponse.success() || tokenResponse.refreshToken() == null) {
            throw new BusinessException("GOOGLE_OAUTH_FAILED",
                "Google ile bağlantı kurulamadı: " + tokenResponse.errorMessage());
        }
        tokenService.consume(stateToken);

        String encryptedRefresh = encryptionService.encrypt(tokenResponse.refreshToken());
        String encryptedAccess = encryptionService.encrypt(tokenResponse.accessToken());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expiresInSeconds());

        CalendarConnection connection = repository.findByEmployeeId(employeeId)
            .orElseGet(() -> CalendarConnection.builder()
                .businessId(businessId)
                .employeeId(employeeId)
                .refreshTokenEncrypted(encryptedRefresh)
                .build());
        connection.updateTokens(encryptedRefresh, encryptedAccess, expiresAt);
        repository.save(connection);
        log.info("Google Takvim bağlantısı kuruldu: employeeId={}", employeeId);
    }

    @Transactional
    public void disconnect(Long businessId, Long employeeId) {
        validateEmployeeInBusiness(businessId, employeeId);
        repository.deleteByEmployeeId(employeeId);
        log.info("Google Takvim bağlantısı kesildi: employeeId={}", employeeId);
    }

    @Transactional(readOnly = true)
    public CalendarConnection findByEmployeeIdOrNull(Long employeeId) {
        return repository.findByEmployeeId(employeeId).orElse(null);
    }

    /**
     * Geçerli bir access token döner; süresi dolmuşsa refresh token ile yeniler.
     * Refresh başarısız olursa (ör. kullanıcı erişimi iptal etmiş) bağlantıyı BROKEN
     * işaretler ve null döner — çağıran senkronu sessizce atlamalıdır.
     */
    @Transactional
    public String resolveAccessToken(CalendarConnection connection) {
        if (connection.isAccessTokenValid()) {
            return encryptionService.decrypt(connection.getAccessTokenEncrypted());
        }
        String refreshToken = encryptionService.decrypt(connection.getRefreshTokenEncrypted());
        GoogleTokenResponse response = googleCalendarClient.refreshAccessToken(refreshToken);
        if (!response.success()) {
            connection.markBroken(response.errorMessage());
            repository.save(connection);
            log.warn("Google Takvim token yenilenemedi, bağlantı BROKEN: employeeId={}, error={}",
                connection.getEmployeeId(), response.errorMessage());
            return null;
        }
        String encryptedAccess = encryptionService.encrypt(response.accessToken());
        String newRefreshEncrypted = response.refreshToken() != null
            ? encryptionService.encrypt(response.refreshToken())
            : null;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(response.expiresInSeconds());
        connection.updateTokens(newRefreshEncrypted, encryptedAccess, expiresAt);
        repository.save(connection);
        return response.accessToken();
    }

    private void validateEmployeeInBusiness(Long businessId, Long employeeId) {
        Employee employee = employeeService.findByIdOrThrow(employeeId);
        if (!employee.getBusinessId().equals(businessId)) {
            throw new BusinessException("EMPLOYEE_NOT_IN_BUSINESS", "Seçilen çalışan bu işletmeye ait değil.");
        }
    }
}
