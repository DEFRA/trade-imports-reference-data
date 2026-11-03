package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.scheduling.annotation.Scheduled;
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
    // Given
    Cache countries = cacheManager.getCache(COUNTRIES_CACHE);
    countries.get("all");
    countries.put("all", List.of("France"));
    countries.get("all");
    countries.get("all");

    // When
    metrics.publish();

    // Then
    assertThat(value("CacheMiss", COUNTRIES_CACHE)).isEqualTo(1);
    assertThat(value("CacheHit", COUNTRIES_CACHE)).isEqualTo(2);
    assertThat(value("CacheMiss", "MDM_POE_CACHE")).isZero();
    assertThat(value("CacheHit", "MDM_POE_CACHE")).isZero();
  }

  @Test
  void publish_shouldReportOnlyWhatHappenedSinceTheLastPublish() {
    // Given
    Cache countries = cacheManager.getCache(COUNTRIES_CACHE);
    countries.get("all");
    metrics.publish();
    int documentsAfterFirstPublish = environment.documents().size();
    countries.put("all", List.of("France"));
    countries.get("all");

    // When
    metrics.publish();

    // Then
    List<JsonNode> secondPublish =
        environment.documents().subList(documentsAfterFirstPublish, environment.documents().size());
    assertThat(value(secondPublish, "CacheMiss", COUNTRIES_CACHE)).isZero();
    assertThat(value(secondPublish, "CacheHit", COUNTRIES_CACHE)).isEqualTo(1);
  }

  @Test
  void publish_shouldEmitNothing_whenEmfIsDisabled() {
    // Given
    var disabledWriter =
        new EmfMetricWriter(false, NAMESPACE, () -> new MetricsLogger(environment));

    // When
    new CacheMetrics(disabledWriter, cacheManager).publish();

    // Then
    assertThat(environment.documents()).isEmpty();
  }

  @Test
  void publish_shouldWarnOnceForEachCacheThatIsMissingOrNotCaffeine() {
    // Given: countries is a plain map cache and ports of entry is missing altogether
    var metricsWithNoCaches =
        new CacheMetrics(writer, new ConcurrentMapCacheManager(COUNTRIES_CACHE));

    try (var logs = CapturingLogAppender.attachedTo(CacheMetrics.class)) {
      // When
      metricsWithNoCaches.publish();
      metricsWithNoCaches.publish();

      // Then
      assertThat(logs.events())
          .extracting(ILoggingEvent::getFormattedMessage)
          .containsExactly(
              "Cache MDM_COUNTRIES_CACHE is missing or is not a Caffeine cache, so it has no metrics",
              "Cache MDM_POE_CACHE is missing or is not a Caffeine cache, so it has no metrics");
    }
    assertThat(environment.documents()).isEmpty();
  }

  @Test
  void publish_shouldBeScheduledEveryMinute() throws NoSuchMethodException {
    // Given
    Method publish = CacheMetrics.class.getMethod("publish");

    // When
    Scheduled annotation = publish.getAnnotation(Scheduled.class);

    // Then
    assertThat(annotation).isNotNull();
    assertThat(annotation.fixedRate()).isEqualTo(60_000L);
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
