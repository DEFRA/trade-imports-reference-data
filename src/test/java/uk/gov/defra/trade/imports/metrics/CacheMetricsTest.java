package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class CacheMetricsTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final CacheMetrics cacheMetrics = new CacheMetrics(meterRegistry);

  @Test
  void statsCounterFor_shouldCountEachHitAndMissTaggedWithTheCacheName() {
    Cache<String, String> cache = cacheCountedBy("MDM_COUNTRIES_CACHE");

    cache.getIfPresent("key");
    cache.put("key", "value");
    cache.getIfPresent("key");
    cache.getIfPresent("key");

    assertThat(count("cache.miss", "MDM_COUNTRIES_CACHE")).isEqualTo(1);
    assertThat(count("cache.hit", "MDM_COUNTRIES_CACHE")).isEqualTo(2);
  }

  @Test
  void statsCounterFor_shouldKeepCaffeinesHitAndMissCounts() {
    Cache<String, String> cache = cacheCountedBy("MDM_POE_CACHE");

    cache.getIfPresent("key");
    cache.put("key", "value");
    cache.getIfPresent("key");

    assertThat(cache.stats().missCount()).isEqualTo(1);
    assertThat(cache.stats().hitCount()).isEqualTo(1);
  }

  @Test
  void statsCounterFor_shouldKeepEachCachesCountsApart() {
    Cache<String, String> countries = cacheCountedBy("MDM_COUNTRIES_CACHE");
    cacheCountedBy("MDM_POE_CACHE");

    countries.getIfPresent("key");

    assertThat(count("cache.miss", "MDM_COUNTRIES_CACHE")).isEqualTo(1);
    assertThat(count("cache.miss", "MDM_POE_CACHE")).isZero();
  }

  private Cache<String, String> cacheCountedBy(String name) {
    return Caffeine.newBuilder().recordStats(() -> cacheMetrics.statsCounterFor(name)).build();
  }

  private double count(String metric, String cache) {
    return meterRegistry.get(metric).tag("cache", cache).counter().count();
  }
}
