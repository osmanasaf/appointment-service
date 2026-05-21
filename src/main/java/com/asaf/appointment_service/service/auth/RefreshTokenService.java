package com.asaf.appointment_service.service.auth;

import com.asaf.appointment_service.entity.auth.RefreshToken;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.repository.auth.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int MAX_TOKENS_PER_USER = 5;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo) {
        // Kullanıcının aktif token sayısını kontrol et
        long activeTokenCount = refreshTokenRepository.countByUserIdAndRevokedFalse(user.getId());
        if (activeTokenCount >= MAX_TOKENS_PER_USER) {
            // En eski token'ları revoke et
            var oldTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
            oldTokens.stream()
                .limit(activeTokenCount - MAX_TOKENS_PER_USER + 1)
                .forEach(RefreshToken::revoke);
            refreshTokenRepository.saveAll(oldTokens);
        }

        RefreshToken refreshToken = RefreshToken.create(user, refreshExpirationDays, deviceInfo);
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        
        log.info("Refresh token oluşturuldu: userId={}", user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    @Transactional
    public RefreshToken verifyAndRotate(String token, String deviceInfo) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz refresh token"));

        if (!refreshToken.isValid()) {
            // Token geçersiz veya revoke edilmiş - güvenlik için tüm token'ları revoke et
            if (refreshToken.isRevoked()) {
                log.warn("Revoke edilmiş token kullanıldı, tüm token'lar revoke ediliyor: userId={}", 
                    refreshToken.getUser().getId());
                refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
            }
            throw new IllegalArgumentException("Refresh token geçersiz veya süresi dolmuş");
        }

        // Eski token'ı revoke et
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // Yeni token oluştur (token rotation)
        return createRefreshToken(refreshToken.getUser(), deviceInfo);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.revoke();
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoke edildi: userId={}", rt.getUser().getId());
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Tüm refresh token'lar revoke edildi: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> listUserTokens(Long userId) {
        return refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void revokeUserToken(Long userId, Long tokenId) {
        refreshTokenRepository.findById(tokenId)
            .filter(token -> token.getUser().getId().equals(userId))
            .ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
                log.info("Kullanıcı oturumu kapatıldı: userId={}, tokenId={}", userId, tokenId);
            });
    }

    @Transactional
    public void revokeOtherUserTokens(Long userId, Long currentTokenId) {
        refreshTokenRepository.revokeAllByUserIdExcept(userId, currentTokenId);
        log.info("Diğer oturumlar kapatıldı: userId={}, keepTokenId={}", userId, currentTokenId);
    }

    @Scheduled(cron = "0 0 3 * * ?") // Her gün saat 03:00'te
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        if (deleted > 0) {
            log.info("Süresi dolmuş/revoke edilmiş {} refresh token silindi", deleted);
        }
    }
}
