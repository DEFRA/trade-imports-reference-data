package uk.gov.defra.trade.imports.metrics;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import uk.gov.defra.trade.imports.configuration.CacheConfig;

/**
 * Publishes {@code CacheHit} and {@code CacheMiss} once a minute for each MDM cache, with the
 * single dimension {@code Cache}. Each value is the number of hits or misses since the last
 * publish, so summing the metric over any period gives the total for that period.
 *
 * <p>A named cache that is missing or is not a Caffeine cache is logged as a warning, once, and
 * skipped, so a renamed cache does not silently stop its metrics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMetrics {

  static final String HIT_METRIC = "CacheHit";
  static final String MISS_METRIC = "CacheMiss";
  static final String CACHE_DIMENSION = "Cache";
  static final List<String> MDM_CACHES =
      List.of(CacheConfig.MDM_COUNTRIES_CACHE, CacheConfig.MDM_POE_CACHE);
  private static final long PUBLISH_INTERVAL_MILLISECONDS = 60_000;

  private final EmfMetricWriter emfMetricWriter;
  private final CacheManager cacheManager;
  private final Map<String, CacheStats> lastPublished = new HashMap<>();
  private final Set<String> warnedAbout = new HashSet<>();

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
      } else if (warnedAbout.add(name)) {
        log.warn("Cache {} is missing or is not a Caffeine cache, so it has no metrics", name);
      }
    }
  }
}
