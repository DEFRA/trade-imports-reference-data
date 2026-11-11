package uk.gov.defra.trade.imports.metrics;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Counts each lookup in a cache as {@code cache.hit} or {@code cache.miss}, tagged {@code cache}
 * with the cache's name. A Caffeine cache built with {@code recordStats(statsCounterFor(name))}
 * counts its own lookups, so a lookup through {@code @Cacheable} is counted too.
 */
@Component
@RequiredArgsConstructor
public class CacheMetrics {

  private final MeterRegistry meterRegistry;

  /**
   * A stats counter for one cache, which also keeps Caffeine's own statistics.
   *
   * @param cacheName the cache's name, used as the {@code cache} tag
   * @return a fresh stats counter for that cache
   */
  public StatsCounter statsCounterFor(String cacheName) {
    return new CountingStatsCounter(
        Counter.builder(MetricNames.CACHE_HIT)
            .tag(MetricNames.CACHE_TAG, cacheName)
            .register(meterRegistry),
        Counter.builder(MetricNames.CACHE_MISS)
            .tag(MetricNames.CACHE_TAG, cacheName)
            .register(meterRegistry));
  }

  private static final class CountingStatsCounter implements StatsCounter {

    private final StatsCounter caffeineStats = new ConcurrentStatsCounter();
    private final Counter hits;
    private final Counter misses;

    private CountingStatsCounter(Counter hits, Counter misses) {
      this.hits = hits;
      this.misses = misses;
    }

    @Override
    public void recordHits(int count) {
      caffeineStats.recordHits(count);
      hits.increment(count);
    }

    @Override
    public void recordMisses(int count) {
      caffeineStats.recordMisses(count);
      misses.increment(count);
    }

    @Override
    public void recordLoadSuccess(long loadTime) {
      caffeineStats.recordLoadSuccess(loadTime);
    }

    @Override
    public void recordLoadFailure(long loadTime) {
      caffeineStats.recordLoadFailure(loadTime);
    }

    @Override
    public void recordEviction(int weight, RemovalCause cause) {
      caffeineStats.recordEviction(weight, cause);
    }

    @Override
    public CacheStats snapshot() {
      return caffeineStats.snapshot();
    }
  }
}
