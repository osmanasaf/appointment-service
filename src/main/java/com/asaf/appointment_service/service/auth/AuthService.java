package com.asaf.appointment_service.service.auth;

import com.asaf.appointment_service.dto.auth.AuthResponse;
import com.asaf.appointment_service.dto.auth.LoginRequest;
import com.asaf.appointment_service.dto.auth.RegisterRequest;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.security.JwtTokenProvider;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
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

    public AuthService(UserRepository userRepository,
                       BusinessService businessService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.businessService = businessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .phoneNumber(request.phoneNumber())
            .businessId(business.getId())
            .role(UserRole.BUSINESS_OWNER)
            .build();

        User savedUser = userRepository.save(user);
        log.info("Yeni kullanıcı kaydedildi: email={}, businessId={}", 
            savedUser.getEmail(), business.getId());

        return generateAuthResponse(savedUser);
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

        log.info("Kullanıcı giriş yaptı: email={}", request.email());
        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String token = tokenProvider.generateToken(
            user.getId(), 
            user.getEmail(), 
            user.getRole().name()
        );

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getBusinessId(),
            user.getRole()
        );

        return AuthResponse.of(token, tokenProvider.getExpirationInSeconds(), userInfo);
    }
}
