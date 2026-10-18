package uk.gov.defra.trade.imports.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Publishes {@code DependencyAvailable} once a minute for each dependency: 1 when it is available
 * and 0 when it is not, with the single dimension {@code Dependency}.
 *
 * <p>This is deliberately not a Spring {@code HealthIndicator}. CDP uses {@code /health} to decide
 * whether a task is alive, so a dependency check there would restart every task whenever a
 * dependency went down.
 *
 * <p>MDM and the Trade token endpoint are not probed, because the MDM caches exist to limit how
 * often we call MDM: their value is the outcome of the most recent real call, and nothing is
 * published for them until there has been one.
 *
 * <p>Because the value is the last outcome, whatever its age, it lags the dependency's real state.
 * A dependency is only called when a request needs it: MDM responses are cached for {@code
 * cache.mdm.ttl-minutes} (60 by default), and {@code MdmClientInterceptor} reuses the token until
 * its own {@code expiresOn}. So the value stays at 1 after a dependency goes down until the next
 * call to it, which with no traffic may never come. A single failed call holds it at 0 until the
 * next call succeeds, however long that takes. This metric
 * is for the dashboard only: the "dependency unavailable" alert is built on {@code
 * ExternalCallFailure} over a time window, per {@code Dependency} and {@code Operation}, which
 * reports only calls that actually happened.
 *
 * <p>MongoDB is deliberately left out. The service has MongoDB configured, but nothing reads or
 * writes through it, so its availability says nothing about whether the service works.
 */
@Component
@RequiredArgsConstructor
public class DependencyAvailabilityMetrics {

  static final String AVAILABLE_METRIC = "DependencyAvailable";
  static final String DEPENDENCY_DIMENSION = "Dependency";
  private static final long PUBLISH_INTERVAL_MILLISECONDS = 60_000;

  private final EmfMetricWriter emfMetricWriter;
  private final ExternalCallMetrics externalCallMetrics;

  @Scheduled(fixedRate = PUBLISH_INTERVAL_MILLISECONDS)
  public void publish() {
    if (!emfMetricWriter.isEnabled()) {
      return;
    }
    externalCallMetrics.lastCallSucceededByDependency().forEach(this::write);
  }

  private void write(String dependency, boolean available) {
    emfMetricWriter.write(
        AVAILABLE_METRIC, available ? 1 : 0, Unit.COUNT, DEPENDENCY_DIMENSION, dependency);
  }
}
