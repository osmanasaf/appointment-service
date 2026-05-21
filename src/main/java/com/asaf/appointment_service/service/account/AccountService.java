package com.asaf.appointment_service.service.account;

import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.account.ChangePasswordRequest;
import com.asaf.appointment_service.dto.account.EmailChangeRequest;
import com.asaf.appointment_service.dto.account.PhoneChangeRequest;
import com.asaf.appointment_service.dto.account.ProfileResponse;
import com.asaf.appointment_service.dto.account.SecuritySessionResponse;
import com.asaf.appointment_service.dto.account.TwoFactorStatusResponse;
import com.asaf.appointment_service.dto.account.UpdateProfileRequest;
import com.asaf.appointment_service.entity.auth.RefreshToken;
import com.asaf.appointment_service.entity.user.OtpType;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.auth.RefreshTokenService;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailClient emailClient;
    private final RefreshTokenService refreshTokenService;
    private final BusinessService businessService;

    public AccountService(CurrentUserService currentUserService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          EmailClient emailClient,
                          RefreshTokenService refreshTokenService,
                          BusinessService businessService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailClient = emailClient;
        this.refreshTokenService = refreshTokenService;
        this.businessService = businessService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        return toProfileResponse(currentUserService.getCurrentUser());
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        user.updateProfile(request.name().trim(), blankToNull(request.phoneNumber()));
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ValidationException("INVALID_CURRENT_PASSWORD", "Mevcut şifre hatalı");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user.getId());
    }

    @Transactional
    public void requestEmailChange(EmailChangeRequest request) {
        User user = currentUserService.getCurrentUser();
        String newEmail = request.email().trim().toLowerCase();
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ValidationException("EMAIL_UNCHANGED", "Yeni e-posta mevcut e-posta ile aynı olamaz");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ValidationException("EMAIL_ALREADY_EXISTS", "Bu e-posta adresi zaten kullanılıyor");
        }
        user.requestEmailChange(newEmail);
        String otp = user.generateOtp(OtpType.EMAIL_CHANGE, 5);
        userRepository.save(user);
        emailClient.sendOtpEmail(newEmail, user.getName(), otp, EmailClient.OtpEmailType.EMAIL_CHANGE);
    }

    @Transactional
    public ProfileResponse confirmEmailChange(EmailChangeRequest.Confirm request) {
        User user = currentUserService.getCurrentUser();
        if (user.getPendingEmail() == null) {
            throw new ValidationException("NO_PENDING_EMAIL", "Bekleyen e-posta değişikliği yok");
        }
        if (!user.verifyOtp(request.otp(), OtpType.EMAIL_CHANGE)) {
            userRepository.save(user);
            throw new ValidationException("INVALID_OTP", "Doğrulama kodu hatalı veya süresi dolmuş");
        }
        user.confirmEmailChange();
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Transactional
    public void requestPhoneChange(PhoneChangeRequest request) {
        User user = currentUserService.getCurrentUser();
        String newPhone = request.phoneNumber().trim();
        user.requestPhoneChange(newPhone);
        String otp = user.generateOtp(OtpType.PHONE_CHANGE, 5);
        userRepository.save(user);
        // SMS sağlayıcısı henüz ayrışmadığı için güvenlik kodu mevcut e-posta adresine gönderilir.
        emailClient.sendOtpEmail(user.getEmail(), user.getName(), otp, EmailClient.OtpEmailType.PHONE_CHANGE);
    }

    @Transactional
    public ProfileResponse confirmPhoneChange(PhoneChangeRequest.Confirm request) {
        User user = currentUserService.getCurrentUser();
        if (user.getPendingPhoneNumber() == null) {
            throw new ValidationException("NO_PENDING_PHONE", "Bekleyen telefon değişikliği yok");
        }
        if (!user.verifyOtp(request.otp(), OtpType.PHONE_CHANGE)) {
            userRepository.save(user);
            throw new ValidationException("INVALID_OTP", "Doğrulama kodu hatalı veya süresi dolmuş");
        }
        user.confirmPhoneChange();
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SecuritySessionResponse> listSessions(String currentRefreshToken) {
        User user = currentUserService.getCurrentUser();
        Long currentTokenId = resolveCurrentTokenId(currentRefreshToken);
        return refreshTokenService.listUserTokens(user.getId()).stream()
            .map(token -> toSessionResponse(token, currentTokenId))
            .toList();
    }

    @Transactional
    public void revokeSession(Long tokenId) {
        User user = currentUserService.getCurrentUser();
        refreshTokenService.revokeUserToken(user.getId(), tokenId);
    }

    @Transactional
    public void revokeOtherSessions(String currentRefreshToken) {
        User user = currentUserService.getCurrentUser();
        Long currentTokenId = resolveCurrentTokenId(currentRefreshToken);
        if (currentTokenId == null) {
            refreshTokenService.revokeAllUserTokens(user.getId());
            return;
        }
        refreshTokenService.revokeOtherUserTokens(user.getId(), currentTokenId);
    }

    public TwoFactorStatusResponse getTwoFactorStatus() {
        return new TwoFactorStatusResponse(false, "none");
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getPhoneNumber(),
            user.getBusinessId(),
            user.getEmployeeId(),
            user.getRole(),
            resolveBusinessSlug(user.getBusinessId()),
            user.isEmailVerified(),
            false
        );
    }

    private SecuritySessionResponse toSessionResponse(RefreshToken token, Long currentTokenId) {
        return new SecuritySessionResponse(
            token.getId(),
            token.getDeviceInfo(),
            token.getCreatedAt(),
            token.getExpiresAt(),
            token.isRevoked(),
            currentTokenId != null && currentTokenId.equals(token.getId())
        );
    }

    private Long resolveCurrentTokenId(String currentRefreshToken) {
        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            return null;
        }
        RefreshToken token = refreshTokenService.findByToken(currentRefreshToken);
        return token != null ? token.getId() : null;
    }

    private String resolveBusinessSlug(Long businessId) {
        if (businessId == null) {
            return null;
        }
        return businessService.findByIdOrThrow(businessId).getSlug();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
