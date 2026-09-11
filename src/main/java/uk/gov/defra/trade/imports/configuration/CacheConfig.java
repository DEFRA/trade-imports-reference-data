package uk.gov.defra.trade.imports.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.mdm.ttl-minutes:60}")
    long mdmCacheTtlMinutes;

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(5)
            .expireAfterWrite(30, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("IDENTITY_TOKEN_CACHE");
        cacheManager.setCaffeine(caffeine);
        // The primary purpose of caching MDM responses is to rate-limit our calls to MDM,
        // rather than to defend against MDM unavailability or improve performance.
        cacheManager.registerCustomCache(
            "MDM_COUNTRIES_CACHE",
            Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(mdmCacheTtlMinutes, TimeUnit.MINUTES)
                .build()
        );
        cacheManager.registerCustomCache(
            "MDM_POE_CACHE",
            Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(mdmCacheTtlMinutes, TimeUnit.MINUTES)
                .build()
        );
        return cacheManager;
    }
}
