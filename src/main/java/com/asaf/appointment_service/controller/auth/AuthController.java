package com.asaf.appointment_service.controller.auth;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.auth.AuthResponse;
import com.asaf.appointment_service.dto.auth.LoginRequest;
import com.asaf.appointment_service.dto.auth.RegisterEmployeeRequest;
import com.asaf.appointment_service.dto.auth.RegisterRequest;
import com.asaf.appointment_service.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 30 gün

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==================== KAYIT ====================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(null, "Kayıt başarılı. Email adresinize doğrulama kodu gönderildi."));
    }

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String deviceInfo = extractDeviceInfo(httpRequest);
        AuthResponse response = authService.verifyRegistrationOtp(request.email(), request.otp(), deviceInfo);
        setRefreshTokenCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Email doğrulandı. Giriş yapıldı."));
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<ApiResponse<Void>> resendRegistrationOtp(
            @Valid @RequestBody EmailRequest request) {
        authService.resendRegistrationOtp(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "Doğrulama kodu yeniden gönderildi."));
    }

    @PostMapping("/register-employee")
    public ResponseEntity<ApiResponse<Void>> registerEmployee(
            @Valid @RequestBody RegisterEmployeeRequest request) {
        authService.registerEmployee(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(null, "Kayıt başarılı. Giriş yapabilirsiniz."));
    }

    // ==================== GİRİŞ ====================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String deviceInfo = extractDeviceInfo(httpRequest);
        AuthResponse response = authService.login(request, deviceInfo);
        setRefreshTokenCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Giriş başarılı"));
    }

    // ==================== TOKEN YENİLEME ====================

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, cookieRefreshToken);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(new com.asaf.appointment_service.common.dto.ApiResponse.ErrorDetails(
                    "MISSING_REFRESH_TOKEN", "Refresh token gerekli", null, java.time.LocalDateTime.now())));
        }
        String deviceInfo = extractDeviceInfo(httpRequest);
        AuthResponse response = authService.refreshToken(refreshToken, deviceInfo);
        setRefreshTokenCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token yenilendi"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, cookieRefreshToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Çıkış yapıldı"));
    }

    // ==================== ESKİ EMAIL DOĞRULAMA (geriye uyumluluk) ====================

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam @NotBlank String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email adresiniz başarıyla doğrulandı."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "Doğrulama emaili gönderildi."));
    }

    // ==================== ŞİFRE SIFIRLAMA ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Eğer bu email kayıtlıysa, şifre sıfırlama kodu gönderildi."));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.verifyPasswordResetOtp(request.email(), request.otp());
        return ResponseEntity.ok(ApiResponse.success(
            new ResetTokenResponse(resetToken), 
            "Kod doğrulandı. Yeni şifrenizi belirleyebilirsiniz."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.resetToken(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Şifreniz başarıyla değiştirildi."));
    }

    // ==================== DTO'lar ====================

    record EmailRequest(
        @NotBlank @Email String email
    ) {}

    record VerifyOtpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 6) String otp
    ) {}

    record ResetTokenResponse(String resetToken) {}

    record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 6) String newPassword
    ) {}

    record RefreshTokenRequest(
        @NotBlank String refreshToken
    ) {}

    // ==================== YARDIMCI METODLAR ====================

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ip = forwardedFor.split(",")[0].trim();
        }
        
        return String.format("IP: %s | UA: %s", ip, 
            userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "unknown");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    private String resolveRefreshToken(RefreshTokenRequest request, String cookieToken) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        return cookieToken;
    }
}
