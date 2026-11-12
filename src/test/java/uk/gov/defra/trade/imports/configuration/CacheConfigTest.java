package uk.gov.defra.trade.imports.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import uk.gov.defra.trade.imports.metrics.CacheMetrics;

class CacheConfigTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final CacheMetrics cacheMetrics = new CacheMetrics(meterRegistry);
    private CacheConfig cacheConfigUnderTest;

    @BeforeEach
    void setUp() {
        cacheConfigUnderTest = new CacheConfig();
        cacheConfigUnderTest.mdmCacheTtlMinutes = 60L;
    }

    @Test
    void caffeineConfig_shouldHoldFiveEntriesForThirtyMinutes() {
        Cache<Object, Object> cache = cacheConfigUnderTest.caffeineConfig().build();

        assertThat(cache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(5);
        assertThat(cache.policy().expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.MINUTES))
            .isEqualTo(30);
    }

    @Test
    void cacheManager_shouldRegisterTheIdentityTokenAndMdmCaches() {
        CacheManager result = cacheConfigUnderTest.cacheManager(Caffeine.newBuilder(), cacheMetrics);
        result.getCache("IDENTITY_TOKEN_CACHE").put("CACHE_KEY", "jwtToken");

        assertThat(result.getCacheNames())
            .containsExactlyInAnyOrder("IDENTITY_TOKEN_CACHE", "MDM_COUNTRIES_CACHE", "MDM_POE_CACHE");
        assertThat(result.getCache("IDENTITY_TOKEN_CACHE").get("CACHE_KEY").get())
            .isEqualTo("jwtToken");
    }

    @ParameterizedTest
    @ValueSource(strings = {CacheConfig.MDM_COUNTRIES_CACHE, CacheConfig.MDM_POE_CACHE})
    void cacheManager_shouldKeepCaffeineStats_whenAnMdmCacheIsRead(String name) {
        CaffeineCache cache = readOnceMissedThenHit(name);

        assertThat(cache.getNativeCache().stats().missCount()).isEqualTo(1);
        assertThat(cache.getNativeCache().stats().hitCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {CacheConfig.MDM_COUNTRIES_CACHE, CacheConfig.MDM_POE_CACHE})
    void cacheManager_shouldCountHitsAndMissesTaggedWithTheCacheName_whenAnMdmCacheIsRead(String name) {
        readOnceMissedThenHit(name);

        assertThat(meterRegistry.get("cache.miss").tag("cache", name).counter().count())
            .isEqualTo(1);
        assertThat(meterRegistry.get("cache.hit").tag("cache", name).counter().count())
            .isEqualTo(1);
    }

    private CaffeineCache readOnceMissedThenHit(String name) {
        CacheManager cacheManager =
            cacheConfigUnderTest.cacheManager(Caffeine.newBuilder(), cacheMetrics);
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(name);
        cache.get("key");
        cache.put("key", "value");
        cache.get("key");
        return cache;
    }
}
