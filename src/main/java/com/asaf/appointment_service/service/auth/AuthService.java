package com.asaf.appointment_service.service.auth;

import com.asaf.appointment_service.dto.auth.AuthResponse;
import com.asaf.appointment_service.dto.auth.LoginRequest;
import com.asaf.appointment_service.dto.auth.RegisterEmployeeRequest;
import com.asaf.appointment_service.dto.auth.RegisterRequest;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.EmployeeInvite;
import com.asaf.appointment_service.entity.user.OtpType;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.exception.auth.EmailNotVerifiedException;
import com.asaf.appointment_service.exception.auth.InvalidOtpException;
import com.asaf.appointment_service.exception.auth.InvalidVerificationTokenException;
import com.asaf.appointment_service.exception.auth.OtpBlockedException;
import com.asaf.appointment_service.entity.auth.RefreshToken;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.security.JwtTokenProvider;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeInviteService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.NicheServiceTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final SubscriptionService subscriptionService;
    private final EmailClient emailClient;
    private final NicheServiceTemplateService nicheServiceTemplateService;
    private final EmployeeInviteService employeeInviteService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       BusinessService businessService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       SubscriptionService subscriptionService,
                       EmailClient emailClient,
                       NicheServiceTemplateService nicheServiceTemplateService,
                       EmployeeInviteService employeeInviteService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.businessService = businessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.subscriptionService = subscriptionService;
        this.emailClient = emailClient;
        this.nicheServiceTemplateService = nicheServiceTemplateService;
        this.employeeInviteService = employeeInviteService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Kayıt işlemini başlatır ve OTP gönderir.
     * Kullanıcı henüz doğrulanmamış olarak kaydedilir.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            // Eğer kullanıcı var ama doğrulanmamışsa, yeni OTP gönder
            User existingUser = userRepository.findByEmail(request.email()).orElseThrow();
            if (!existingUser.isEmailVerified()) {
                String otp = existingUser.generateOtp(OtpType.EMAIL_VERIFICATION, 5);
                userRepository.save(existingUser);
                emailClient.sendOtpEmail(existingUser.getEmail(), existingUser.getName(), otp, 
                    EmailClient.OtpEmailType.EMAIL_VERIFICATION);
                log.info("Mevcut doğrulanmamış kullanıcıya yeni OTP gönderildi: email={}", request.email());
                return;
            }
            throw new IllegalArgumentException("Bu email zaten kayıtlı");
        }

        String phone = request.phoneNumber() != null ? request.phoneNumber().trim() : "";
        if (phone.isBlank()) {
            phone = "0000000000";
        }

        Business business = businessService.createBusiness(new CreateBusinessRequest(
            request.businessName(),
            phone,
            "+90",
            request.email(),
            null,
            null,
            request.businessCategory()
        ));
        subscriptionService.initializeTrialSubscription(business.getId());
        nicheServiceTemplateService.seedServicesForCategory(business.getId(), request.businessCategory());

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .phoneNumber(request.phoneNumber())
            .businessId(business.getId())
            .role(UserRole.BUSINESS_OWNER)
            .build();

        String otp = user.generateOtp(OtpType.EMAIL_VERIFICATION, 5);
        User savedUser = userRepository.save(user);

        log.info("Yeni kullanıcı kaydedildi (OTP bekleniyor): userId={}, businessId={}", 
            savedUser.getId(), business.getId());

        emailClient.sendOtpEmail(savedUser.getEmail(), savedUser.getName(), otp, 
            EmailClient.OtpEmailType.EMAIL_VERIFICATION);
    }

    /**
     * Email doğrulama OTP'sini kontrol eder ve hesabı aktifleştirir.
     */
    @Transactional
    public AuthResponse verifyRegistrationOtp(String email, String otpCode, String deviceInfo) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email zaten doğrulanmış");
        }

        if (user.isOtpBlocked()) {
            throw new OtpBlockedException(user.getOtpBlockedUntil());
        }

        if (!user.verifyOtp(otpCode, OtpType.EMAIL_VERIFICATION)) {
            userRepository.save(user);
            if (user.isOtpBlocked()) {
                throw new OtpBlockedException(user.getOtpBlockedUntil());
            }
            throw new InvalidOtpException();
        }

        user.verifyEmail();
        userRepository.save(user);

        log.info("Email OTP ile doğrulandı: userId={}", user.getId());
        
        return generateAuthResponse(user, deviceInfo);
    }

    /**
     * Kayıt OTP'sini yeniden gönderir.
     */
    @Transactional
    public void resendRegistrationOtp(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Bu email ile kayıtlı kullanıcı bulunamadı"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email zaten doğrulanmış");
        }

        String otp = user.generateOtp(OtpType.EMAIL_VERIFICATION, 5);
        userRepository.save(user);

        emailClient.sendOtpEmail(user.getEmail(), user.getName(), otp, 
            EmailClient.OtpEmailType.EMAIL_VERIFICATION);
        log.info("Kayıt OTP'si yeniden gönderildi: userId={}", user.getId());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new InvalidVerificationTokenException("Doğrulama linki geçersiz veya kullanılmış."));

        if (user.isVerificationTokenExpired()) {
            throw new InvalidVerificationTokenException("Doğrulama linkinin süresi dolmuş. Lütfen yeni link isteyin.");
        }

        user.confirmEmailVerification();
        userRepository.save(user);

        log.info("Email doğrulandı: userId={}", user.getId());
    }

    @Transactional
    public void registerEmployee(RegisterEmployeeRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Bu email zaten kayıtlı");
        }

        // Davet kodunu kontrol et
        EmployeeInvite invite = employeeInviteService.findByTokenOrThrow(request.inviteToken());
        
        if (!invite.isValid()) {
            throw new IllegalArgumentException("Davet kodu geçersiz veya süresi dolmuş");
        }

        if (!invite.getEmail().equalsIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Bu davet kodu bu email için geçerli değil");
        }

        employeeInviteService.assertRegistrationNameMatchesInvite(invite, request.name());

        // Kullanıcı oluştur
        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .phoneNumber(request.phoneNumber())
            .businessId(invite.getBusinessId())
            .employeeId(invite.getEmployeeId())
            .role(UserRole.EMPLOYEE)
            .build();

        // Email doğrulamasını atla (davet ile geldi)
        user.verifyEmail();
        User savedUser = userRepository.save(user);

        // Daveti kullanıldı olarak işaretle
        employeeInviteService.markAsUsed(request.inviteToken());

        log.info("Çalışan kaydedildi: userId={}, employeeId={}, businessId={}", 
            savedUser.getId(), invite.getEmployeeId(), invite.getBusinessId());
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Bu email ile kayıtlı kullanıcı bulunamadı"));

        if (user.isEmailVerified()) {
            return;
        }

        String newToken = user.initiateEmailVerification();
        userRepository.save(user);

        emailClient.sendVerificationEmail(user.getEmail(), user.getName(), newToken);
        log.info("Doğrulama emaili yeniden gönderildi: userId={}", user.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Hesabınız aktif değil");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        log.info("Kullanıcı giriş yaptı: userId={}", user.getId());
        return generateAuthResponse(user, deviceInfo);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr, String deviceInfo) {
        RefreshToken refreshToken = refreshTokenService.verifyAndRotate(refreshTokenStr, deviceInfo);
        User user = refreshToken.getUser();

        if (!user.isActive()) {
            throw new IllegalArgumentException("Hesabınız aktif değil");
        }

        log.info("Token yenilendi: userId={}", user.getId());
        
        String accessToken = tokenProvider.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getBusinessId(),
            user.getEmployeeId()
        );

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getBusinessId(),
            user.getEmployeeId(),
            user.getRole()
        );

        return AuthResponse.of(accessToken, refreshToken.getToken(), tokenProvider.getExpirationInSeconds(), userInfo);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenService.revokeToken(refreshTokenStr);
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    // ==================== ŞİFRE SIFIRLAMA ====================

    /**
     * Şifre sıfırlama OTP'si gönderir.
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        // Güvenlik: Kullanıcı yoksa bile aynı mesajı döndür
        if (user == null) {
            log.info("Şifre sıfırlama istendi ama kullanıcı bulunamadı: email={}", email);
            return;
        }

        String otp = user.generateOtp(OtpType.PASSWORD_RESET, 5);
        userRepository.save(user);

        emailClient.sendOtpEmail(user.getEmail(), user.getName(), otp, 
            EmailClient.OtpEmailType.PASSWORD_RESET);
        log.info("Şifre sıfırlama OTP'si gönderildi: userId={}", user.getId());
    }

    /**
     * Şifre sıfırlama OTP'sini doğrular ve geçici token döndürür.
     */
    @Transactional
    public String verifyPasswordResetOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidOtpException::new);

        if (user.isOtpBlocked()) {
            throw new OtpBlockedException(user.getOtpBlockedUntil());
        }

        if (!user.verifyOtp(otpCode, OtpType.PASSWORD_RESET)) {
            userRepository.save(user);
            if (user.isOtpBlocked()) {
                throw new OtpBlockedException(user.getOtpBlockedUntil());
            }
            throw new InvalidOtpException();
        }

        String resetToken = user.generatePasswordResetToken();
        userRepository.save(user);

        log.info("Şifre sıfırlama OTP doğrulandı: userId={}", user.getId());
        return resetToken;
    }

    /**
     * Yeni şifreyi ayarlar.
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findByPasswordResetToken(resetToken)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz veya süresi dolmuş token"));

        if (!user.verifyPasswordResetToken(resetToken)) {
            throw new IllegalArgumentException("Geçersiz veya süresi dolmuş token");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken();
        userRepository.save(user);

        log.info("Şifre sıfırlandı: userId={}", user.getId());
    }

    private AuthResponse generateAuthResponse(User user, String deviceInfo) {
        String accessToken = tokenProvider.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getBusinessId(),
            user.getEmployeeId()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceInfo);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getBusinessId(),
            user.getEmployeeId(),
            user.getRole()
        );

        return AuthResponse.of(accessToken, refreshToken.getToken(), tokenProvider.getExpirationInSeconds(), userInfo);
    }
}
