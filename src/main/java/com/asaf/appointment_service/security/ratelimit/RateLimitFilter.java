package com.asaf.appointment_service.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties properties;
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> publicBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> webhookBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        
        Bucket bucket = resolveBucket(path, clientIp);
        
        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            long remainingTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit aşıldı: ip={}, path={}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.getWriter().write("""
                {"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later."}}
                """);
        }
    }

    private Bucket resolveBucket(String path, String clientIp) {
        if (path.startsWith("/api/v1/auth/")) {
            return authBuckets.computeIfAbsent(clientIp, this::createAuthBucket);
        }
        if (path.startsWith("/api/v1/public/")) {
            return publicBuckets.computeIfAbsent(clientIp, this::createPublicBucket);
        }
        if (path.startsWith("/api/v1/webhooks/")) {
            return webhookBuckets.computeIfAbsent(clientIp, this::createWebhookBucket);
        }
        return null;
    }

    private Bucket createAuthBucket(String key) {
        int rpm = properties.auth().requestsPerMinute();
        return Bucket.builder()
            .addLimit(Bandwidth.classic(rpm, Refill.greedy(rpm, Duration.ofMinutes(1))))
            .build();
    }

    private Bucket createPublicBucket(String key) {
        int rpm = properties.publicApi().requestsPerMinute();
        return Bucket.builder()
            .addLimit(Bandwidth.classic(rpm, Refill.greedy(rpm, Duration.ofMinutes(1))))
            .build();
    }

    private Bucket createWebhookBucket(String key) {
        int rpm = properties.webhook().requestsPerMinute();
        return Bucket.builder()
            .addLimit(Bandwidth.classic(rpm, Refill.greedy(rpm, Duration.ofMinutes(1))))
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console") 
            || path.startsWith("/actuator")
            || path.equals("/favicon.ico");
    }
}
