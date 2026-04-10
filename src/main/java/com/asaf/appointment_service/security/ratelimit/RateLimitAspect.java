package com.asaf.appointment_service.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimitProperties properties;
    private final RateLimiterStore store;

    public RateLimitAspect(RateLimitProperties properties, RateLimiterStore store) {
        this.properties = properties;
        this.store = store;
    }

    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        if (!properties.enabled()) {
            return joinPoint.proceed();
        }

        String key = buildKey(rateLimited.type());
        int maxRequests = getMaxRequests(rateLimited.type());
        long windowMillis = getWindowMillis(rateLimited.type());

        if (!store.tryConsume(key, maxRequests, windowMillis)) {
            log.warn("Rate limit exceeded: type={}, key={}", rateLimited.type(), key);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla istek. Lütfen daha sonra tekrar deneyin.");
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimitType type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth != null && auth.isAuthenticated() ? auth.getName() : getClientIp();
        return type.name() + ":" + identifier;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private int getMaxRequests(RateLimitType type) {
        return switch (type) {
            case AUTH -> properties.auth().requestsPerMinute();
            case PUBLIC_API -> properties.publicApi().requestsPerMinute();
            case WEBHOOK -> properties.webhook().requestsPerMinute();
            case INVITE -> properties.invite().requestsPerHour();
        };
    }

    private long getWindowMillis(RateLimitType type) {
        return switch (type) {
            case AUTH, PUBLIC_API, WEBHOOK -> 60_000L; // 1 minute
            case INVITE -> 3_600_000L; // 1 hour
        };
    }
}
