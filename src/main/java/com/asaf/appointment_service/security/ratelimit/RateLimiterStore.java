package com.asaf.appointment_service.security.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterStore {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int maxRequests, long windowMillis) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxRequests, windowMillis));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int maxRequests;
        private final long windowMillis;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        TokenBucket(int maxRequests, long windowMillis) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowMillis;
            this.tokens = new AtomicInteger(maxRequests);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        synchronized boolean tryConsume() {
            refillIfNeeded();
            int current = tokens.get();
            if (current > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            if (now - lastRefill >= windowMillis) {
                tokens.set(maxRequests);
                lastRefillTime.set(now);
            }
        }
    }
}
