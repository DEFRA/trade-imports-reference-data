package uk.gov.defra.trade.imports.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Publishes {@code DependencyAvailable} once a minute for each dependency: 1 when it is available
 * and 0 when it is not, with the single dimension {@code Dependency}. The value is the outcome of
 * the most recent real call to the dependency, whatever its age; nothing is published for a
 * dependency until it has been called.
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
