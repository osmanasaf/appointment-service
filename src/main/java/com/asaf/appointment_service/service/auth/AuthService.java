package com.asaf.appointment_service.service.auth;

import com.asaf.appointment_service.dto.auth.AuthResponse;
import com.asaf.appointment_service.dto.auth.LoginRequest;
import com.asaf.appointment_service.dto.auth.RegisterRequest;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.exception.auth.EmailNotVerifiedException;
import com.asaf.appointment_service.exception.auth.InvalidVerificationTokenException;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.security.JwtTokenProvider;
import com.asaf.appointment_service.service.business.BusinessService;
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

    public AuthService(UserRepository userRepository,
                       BusinessService businessService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       SubscriptionService subscriptionService,
                       EmailClient emailClient,
                       NicheServiceTemplateService nicheServiceTemplateService) {
        this.userRepository = userRepository;
        this.businessService = businessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.subscriptionService = subscriptionService;
        this.emailClient = emailClient;
        this.nicheServiceTemplateService = nicheServiceTemplateService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
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

        String verificationToken = user.initiateEmailVerification();
        User savedUser = userRepository.save(user);

        log.info("Yeni kullanıcı kaydedildi: userId={}, businessId={}", savedUser.getId(), business.getId());

        emailClient.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), verificationToken);
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
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
        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String token = tokenProvider.generateToken(
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

        return AuthResponse.of(token, tokenProvider.getExpirationInSeconds(), userInfo);
    }
}
