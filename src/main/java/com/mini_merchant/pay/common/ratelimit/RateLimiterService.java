package com.mini_merchant.pay.common.ratelimit;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimiterService implements IRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final String KEY_PREFIX = "ratelimit:payments:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.ratelimit.payments-limit:10}")
    private int limit;

    @Value("${app.ratelimit.payments-window-seconds:60}")
    private long windowSeconds;

    @Override
    public boolean tryAcquire(UUID merchantId) {
        String key = KEY_PREFIX + merchantId;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true; // defensive fail-open
            }
            if (count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count <= limit;
        } catch (RuntimeException ex) {
            // Fail-open: never block payments because Redis is unavailable.
            log.warn("Rate limiter unavailable for merchant {}, allowing request", merchantId, ex);
            return true;
        }
    }
}
