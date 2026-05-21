package com.asaf.appointment_service.controller.account;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.account.ChangePasswordRequest;
import com.asaf.appointment_service.dto.account.EmailChangeRequest;
import com.asaf.appointment_service.dto.account.PhoneChangeRequest;
import com.asaf.appointment_service.dto.account.ProfileResponse;
import com.asaf.appointment_service.dto.account.SecuritySessionResponse;
import com.asaf.appointment_service.dto.account.TwoFactorStatusResponse;
import com.asaf.appointment_service.dto.account.UpdateProfileRequest;
import com.asaf.appointment_service.service.account.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.updateProfile(request), "Profil güncellendi"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Şifre güncellendi"));
    }

    @PostMapping("/email-change/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @Valid @RequestBody EmailChangeRequest request) {
        accountService.requestEmailChange(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Yeni e-posta adresine doğrulama kodu gönderildi"));
    }

    @PostMapping("/email-change/confirm")
    public ResponseEntity<ApiResponse<ProfileResponse>> confirmEmailChange(
            @Valid @RequestBody EmailChangeRequest.Confirm request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.confirmEmailChange(request), "E-posta güncellendi"));
    }

    @PostMapping("/phone-change/request")
    public ResponseEntity<ApiResponse<Void>> requestPhoneChange(
            @Valid @RequestBody PhoneChangeRequest request) {
        accountService.requestPhoneChange(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Telefon değişikliği için doğrulama kodu gönderildi"));
    }

    @PostMapping("/phone-change/confirm")
    public ResponseEntity<ApiResponse<ProfileResponse>> confirmPhoneChange(
            @Valid @RequestBody PhoneChangeRequest.Confirm request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.confirmPhoneChange(request), "Telefon güncellendi"));
    }

    @GetMapping("/security/sessions")
    public ResponseEntity<ApiResponse<List<SecuritySessionResponse>>> listSessions(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success(accountService.listSessions(refreshToken)));
    }

    @DeleteMapping("/security/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable Long id) {
        accountService.revokeSession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Oturum kapatıldı"));
    }

    @PostMapping("/security/sessions/revoke-others")
    public ResponseEntity<ApiResponse<Void>> revokeOtherSessions(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        accountService.revokeOtherSessions(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Diğer oturumlar kapatıldı"));
    }

    @GetMapping("/security/2fa")
    public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> getTwoFactorStatus() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getTwoFactorStatus()));
    }
}
