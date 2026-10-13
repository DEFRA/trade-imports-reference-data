package uk.gov.defra.trade.imports.metrics;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Publishes {@code CacheHit} and {@code CacheMiss} once a minute for each MDM cache, with the
 * single dimension {@code Cache}. Each value is the number of hits or misses since the last
 * publish, so summing the metric over any period gives the total for that period.
 */
@Component
@RequiredArgsConstructor
public class CacheMetrics {

  static final String HIT_METRIC = "CacheHit";
  static final String MISS_METRIC = "CacheMiss";
  static final String CACHE_DIMENSION = "Cache";
  static final List<String> MDM_CACHES = List.of("MDM_COUNTRIES_CACHE", "MDM_POE_CACHE");
  private static final long PUBLISH_INTERVAL_MILLISECONDS = 60_000;

  private final EmfMetricWriter emfMetricWriter;
  private final CacheManager cacheManager;
  private final Map<String, CacheStats> lastPublished = new HashMap<>();

  @Scheduled(fixedRate = PUBLISH_INTERVAL_MILLISECONDS)
  public void publish() {
    if (!emfMetricWriter.isEnabled()) {
      return;
    }
    for (String name : MDM_CACHES) {
      if (cacheManager.getCache(name) instanceof CaffeineCache cache) {
        CacheStats total = cache.getNativeCache().stats();
        CacheStats sinceLastPublish =
            total.minus(lastPublished.getOrDefault(name, CacheStats.empty()));
        lastPublished.put(name, total);
        emfMetricWriter.write(
            HIT_METRIC, (double) sinceLastPublish.hitCount(), Unit.COUNT, CACHE_DIMENSION, name);
        emfMetricWriter.write(
            MISS_METRIC, (double) sinceLastPublish.missCount(), Unit.COUNT, CACHE_DIMENSION, name);
      }
    }
  }
}
