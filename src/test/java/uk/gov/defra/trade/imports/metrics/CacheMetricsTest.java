package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

class CacheMetricsTest {

  private static final String NAMESPACE = "trade-imports-reference-data";
  private static final String COUNTRIES_CACHE = "MDM_COUNTRIES_CACHE";

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final EmfMetricWriter writer =
      new EmfMetricWriter(true, NAMESPACE, () -> new MetricsLogger(environment));
  private final CaffeineCacheManager cacheManager = cacheManagerRecordingStats();
  private final CacheMetrics metrics = new CacheMetrics(writer, cacheManager);

  @Test
  void publish_shouldReportHitsAndMissesForEachMdmCache() {
    Cache countries = cacheManager.getCache(COUNTRIES_CACHE);
    countries.get("all");
    countries.put("all", List.of("France"));
    countries.get("all");
    countries.get("all");

    metrics.publish();

    assertThat(value("CacheMiss", COUNTRIES_CACHE)).isEqualTo(1);
    assertThat(value("CacheHit", COUNTRIES_CACHE)).isEqualTo(2);
    assertThat(value("CacheMiss", "MDM_POE_CACHE")).isZero();
    assertThat(value("CacheHit", "MDM_POE_CACHE")).isZero();
  }

  @Test
  void publish_shouldReportOnlyWhatHappenedSinceTheLastPublish() {
    Cache countries = cacheManager.getCache(COUNTRIES_CACHE);
    countries.get("all");
    metrics.publish();
    int documentsAfterFirstPublish = environment.documents().size();
    countries.put("all", List.of("France"));
    countries.get("all");

    metrics.publish();

    List<JsonNode> secondPublish =
        environment.documents().subList(documentsAfterFirstPublish, environment.documents().size());
    assertThat(value(secondPublish, "CacheMiss", COUNTRIES_CACHE)).isZero();
    assertThat(value(secondPublish, "CacheHit", COUNTRIES_CACHE)).isEqualTo(1);
  }

  @Test
  void publish_shouldEmitNothing_whenEmfIsDisabled() {
    var disabledWriter =
        new EmfMetricWriter(false, NAMESPACE, () -> new MetricsLogger(environment));

    new CacheMetrics(disabledWriter, cacheManager).publish();

    assertThat(environment.documents()).isEmpty();
  }

  private int value(String metric, String cache) {
    return value(environment.documents(), metric, cache);
  }

  private static int value(List<JsonNode> documents, String metric, String cache) {
    return documents.stream()
        .filter(document -> document.has(metric))
        .filter(document -> cache.equals(document.get("Cache").asText()))
        .findFirst()
        .orElseThrow()
        .get(metric)
        .asInt();
  }

  private static CaffeineCacheManager cacheManagerRecordingStats() {
    var manager = new CaffeineCacheManager();
    CacheMetrics.MDM_CACHES.forEach(
        name -> manager.registerCustomCache(name, Caffeine.newBuilder().recordStats().build()));
    return manager;
  }
}
