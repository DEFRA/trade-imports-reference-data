package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
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
    // When
    metrics.publish();

    // Then
    assertThat(environment.documents()).isEmpty();
  }

  @Test
  void publish_shouldReportEachCalledDependency_byItsMostRecentCall() {
    // Given
    externalCallMetrics.recordCall(ExternalCall.TRADE_TOKEN, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(
        ExternalCall.MDM_GET_PORTS_OF_ENTRY, Duration.ofMillis(5), true);

    // When
    metrics.publish();

    // Then
    assertThat(availabilityByDependency())
        .containsOnly(Map.entry("trade-token", 1), Map.entry("mdm", 0));
  }

  @Test
  void publish_shouldReportADependencyAvailableAgain_whenACallSucceedsAfterAFailure() {
    // Given
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), true);
    externalCallMetrics.recordCall(
        ExternalCall.MDM_GET_PORTS_OF_ENTRY, Duration.ofMillis(5), false);

    // When
    metrics.publish();

    // Then
    assertThat(availabilityByDependency()).containsOnly(Map.entry("mdm", 1));
  }

  @Test
  void publish_shouldEmitOnlyTheDependencyDimension() {
    // Given
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);

    // When
    metrics.publish();

    // Then
    JsonNode directive =
        environment.documents().getFirst().get("_aws").get("CloudWatchMetrics").get(0);
    assertThat(directive.get("Dimensions")).hasSize(1);
    assertThat(directive.get("Dimensions").get(0)).hasSize(1);
    assertThat(directive.get("Dimensions").get(0).get(0).asText()).isEqualTo("Dependency");
  }

  @Test
  void publish_shouldReportNothing_whenEmfIsDisabled() {
    // Given
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);
    var disabledWriter =
        new EmfMetricWriter(false, NAMESPACE, () -> new MetricsLogger(environment));
    var disabled = new DependencyAvailabilityMetrics(disabledWriter, externalCallMetrics);

    // When
    disabled.publish();

    // Then
    assertThat(environment.documents()).isEmpty();
  }

  @Test
  void publish_shouldBeScheduledEveryMinute() throws NoSuchMethodException {
    // Given
    Method publish = DependencyAvailabilityMetrics.class.getMethod("publish");

    // When
    Scheduled annotation = publish.getAnnotation(Scheduled.class);

    // Then
    assertThat(annotation).isNotNull();
    assertThat(annotation.fixedRate()).isEqualTo(60_000L);
  }

  private Map<String, Integer> availabilityByDependency() {
    return environment.documents().stream()
        .collect(
            Collectors.toMap(
                document -> document.get("Dependency").asText(),
                document -> document.get("DependencyAvailable").asInt()));
  }
}
