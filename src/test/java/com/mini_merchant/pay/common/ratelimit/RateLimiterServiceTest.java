package com.mini_merchant.pay.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiterService rateLimiterService;

    private final UUID merchantId = UUID.randomUUID();
    private String key;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(stringRedisTemplate);
        ReflectionTestUtils.setField(rateLimiterService, "limit", 10);
        ReflectionTestUtils.setField(rateLimiterService, "windowSeconds", 60L);
        key = "ratelimit:payments:" + merchantId;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void firstRequest_returnsTrueAndSetsExpiry() {
        when(valueOperations.increment(key)).thenReturn(1L);

        assertThat(rateLimiterService.tryAcquire(merchantId)).isTrue();
        verify(stringRedisTemplate).expire(key, Duration.ofSeconds(60L));
    }

    @Test
    void midWindowRequest_returnsTrueAndDoesNotResetExpiry() {
        when(valueOperations.increment(key)).thenReturn(5L);

        assertThat(rateLimiterService.tryAcquire(merchantId)).isTrue();
        verify(stringRedisTemplate, never()).expire(any(), any(Duration.class));
    }

    @Test
    void atLimit_returnsTrue() {
        when(valueOperations.increment(key)).thenReturn(10L);

        assertThat(rateLimiterService.tryAcquire(merchantId)).isTrue();
    }

    @Test
    void overLimit_returnsFalse() {
        when(valueOperations.increment(key)).thenReturn(11L);

        assertThat(rateLimiterService.tryAcquire(merchantId)).isFalse();
    }

    @Test
    void nullIncrement_failsOpenReturnsTrue() {
        when(valueOperations.increment(key)).thenReturn(null);

        assertThat(rateLimiterService.tryAcquire(merchantId)).isTrue();
        verify(stringRedisTemplate, never()).expire(eq(key), any(Duration.class));
    }

    @Test
    void redisThrows_failsOpenReturnsTrue() {
        when(valueOperations.increment(key)).thenThrow(new RuntimeException("redis down"));

        assertThat(rateLimiterService.tryAcquire(merchantId)).isTrue();
    }
}
