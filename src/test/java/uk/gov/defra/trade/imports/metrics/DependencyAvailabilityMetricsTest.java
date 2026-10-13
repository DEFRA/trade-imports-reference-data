package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

class DependencyAvailabilityMetricsTest {

  private static final String NAMESPACE = "trade-imports-reference-data";

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final EmfMetricWriter writer =
      new EmfMetricWriter(true, NAMESPACE, () -> new MetricsLogger(environment));
  private final ExternalCallMetrics externalCallMetrics =
      new ExternalCallMetrics(false, NAMESPACE, () -> new MetricsLogger(environment));
  private final DependencyAvailabilityMetrics metrics =
      new DependencyAvailabilityMetrics(writer, externalCallMetrics);

  @Test
  void publish_shouldReportNothing_beforeAnyDependencyHasBeenCalled() {
    metrics.publish();

    assertThat(environment.documents()).isEmpty();
  }

  @Test
  void publish_shouldReportEachCalledDependency_byItsMostRecentCall() {
    externalCallMetrics.recordCall(ExternalCall.TRADE_TOKEN, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(
        ExternalCall.MDM_GET_PORTS_OF_ENTRY, Duration.ofMillis(5), true);

    metrics.publish();

    assertThat(availabilityByDependency())
        .containsOnly(Map.entry("trade-token", 1), Map.entry("mdm", 0));
  }

  @Test
  void publish_shouldEmitOnlyTheDependencyDimension() {
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);

    metrics.publish();

    JsonNode directive =
        environment.documents().getFirst().get("_aws").get("CloudWatchMetrics").get(0);
    assertThat(directive.get("Dimensions")).hasSize(1);
    assertThat(directive.get("Dimensions").get(0)).hasSize(1);
    assertThat(directive.get("Dimensions").get(0).get(0).asText()).isEqualTo("Dependency");
  }

  @Test
  void publish_shouldReportNothing_whenEmfIsDisabled() {
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);
    var disabledWriter =
        new EmfMetricWriter(false, NAMESPACE, () -> new MetricsLogger(environment));
    var disabled = new DependencyAvailabilityMetrics(disabledWriter, externalCallMetrics);

    disabled.publish();

    assertThat(environment.documents()).isEmpty();
  }

  private Map<String, Integer> availabilityByDependency() {
    return environment.documents().stream()
        .collect(
            Collectors.toMap(
                document -> document.get("Dependency").asText(),
                document -> document.get("DependencyAvailable").asInt()));
  }
}
