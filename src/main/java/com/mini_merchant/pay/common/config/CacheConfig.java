package com.mini_merchant.pay.common.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.mini_merchant.pay.entity.Merchants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${app.cache.merchant-ttl-minutes:10}")
    private long merchantTtlMinutes;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON (not JDK) value serialization: carries no classloader identity on the wire, so it
        // survives DevTools' RestartClassLoader and entity-shape changes across deploys.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Merchants> valueSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, Merchants.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(merchantTtlMinutes))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Degrade gracefully when Redis is unavailable: log a concise warning and fall through to the
     * underlying method (e.g. the Postgres merchant lookup) instead of throwing. Without this, a Redis
     * outage turns every @Cacheable payment auth into an HTTP 500. It also lets you stress test with
     * Redis stopped to measure the no-cache (Postgres-only) path.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                logCacheBypass("GET", cache, ex);
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                logCacheBypass("PUT", cache, ex);
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                logCacheBypass("EVICT", cache, ex);
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                logCacheBypass("CLEAR", cache, ex);
            }
        };
    }

    // One concise line (message only, NO stack trace) so a Redis outage does not flood the logs.
    // A full stack trace per request would dominate I/O and skew load-test numbers.
    private static void logCacheBypass(String op, Cache cache, RuntimeException ex) {
        log.warn("Cache {} bypassed on '{}' (Redis unavailable): {}", op, cache.getName(), ex.getMessage());
    }
}
