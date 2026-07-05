package com.mini_merchant.pay.repository.merchants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.mini_merchant.pay.entity.Merchants;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MerchantRepositoryCacheTest.CacheTestConfig.class)
class MerchantRepositoryCacheTest {

    private static final String API_KEY = "cache-key";
    private static final String CACHE_NAME = "merchantByApiKey";

    @EnableCaching
    @Configuration
    static class CacheTestConfig {

        @Bean
        IMerchantJpaRepository iMerchantJpaRepository() {
            return Mockito.mock(IMerchantJpaRepository.class);
        }

        @Bean
        IMerchantRepository merchantRepository(IMerchantJpaRepository jpa) {
            return new MerchantRepository(jpa);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CACHE_NAME);
        }
    }

    @Autowired
    private IMerchantJpaRepository iMerchantJpaRepository;

    @Autowired
    private IMerchantRepository merchantRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void reset() {
        cacheManager.getCache(CACHE_NAME).clear();
        Mockito.reset(iMerchantJpaRepository);
    }

    private Merchants merchant() {
        Merchants m = new Merchants();
        m.setId(UUID.randomUUID());
        m.setApiKey(API_KEY);
        m.setSecret("secret");
        m.setIsDeleted(false);
        return m;
    }

    @Test
    void findByApiKey_present_isCached_secondCallSkipsJpa() {
        when(iMerchantJpaRepository.findByApiKeyAndIsDeletedFalse(API_KEY))
                .thenReturn(Optional.of(merchant()));

        assertThat(merchantRepository.findByApiKey(API_KEY)).isPresent();
        assertThat(merchantRepository.findByApiKey(API_KEY)).isPresent();

        verify(iMerchantJpaRepository, times(1)).findByApiKeyAndIsDeletedFalse(API_KEY);
    }

    @Test
    void findByApiKey_empty_notCached_hitsJpaEachTime() {
        when(iMerchantJpaRepository.findByApiKeyAndIsDeletedFalse(API_KEY))
                .thenReturn(Optional.empty());

        assertThat(merchantRepository.findByApiKey(API_KEY)).isEmpty();
        assertThat(merchantRepository.findByApiKey(API_KEY)).isEmpty();

        verify(iMerchantJpaRepository, times(2)).findByApiKeyAndIsDeletedFalse(API_KEY);
    }
}