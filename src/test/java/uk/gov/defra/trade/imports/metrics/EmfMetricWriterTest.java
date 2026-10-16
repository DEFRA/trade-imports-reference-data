package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

class EmfMetricWriterTest {

  private static final String NAMESPACE = "trade-imports-reference-data";

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final EmfMetricWriter writer =
      new EmfMetricWriter(true, NAMESPACE, () -> new MetricsLogger(environment));

  @Test
  void write_shouldEmitOneDocumentWithTheMetricAndItsOnlyDimension() {
    writer.write("Lookup", 1, Unit.COUNT, "LookupType", "countries");

    assertThat(environment.documents()).hasSize(1);
    JsonNode document = environment.documents().getFirst();
    JsonNode directive = document.get("_aws").get("CloudWatchMetrics").get(0);
    assertThat(directive.get("Namespace").asText()).isEqualTo(NAMESPACE);
    assertThat(directive.get("Dimensions")).hasSize(1);
    assertThat(directive.get("Dimensions").get(0)).hasSize(1);
    assertThat(directive.get("Dimensions").get(0).get(0).asText()).isEqualTo("LookupType");
    assertThat(directive.get("Metrics")).hasSize(1);
    assertThat(directive.get("Metrics").get(0).get("Name").asText()).isEqualTo("Lookup");
    assertThat(directive.get("Metrics").get(0).get("Unit").asText()).isEqualTo("Count");
    assertThat(document.get("LookupType").asText()).isEqualTo("countries");
    assertThat(document.get("Lookup").asInt()).isEqualTo(1);
    assertThat(document.has("ServiceName")).isFalse();
  }

  @Test
  void write_shouldLogAWarningAndNotThrow_whenTheSinkFails() {
    // Given
    var failingEnvironment = new CapturingEmfEnvironment(true);
    var failingWriter =
        new EmfMetricWriter(true, NAMESPACE, () -> new MetricsLogger(failingEnvironment));

    try (var logs = CapturingLogAppender.attachedTo(EmfMetricWriter.class)) {
      // When & Then
      assertThatCode(
              () -> failingWriter.write("Lookup", 1, Unit.COUNT, "LookupType", "countries"))
          .doesNotThrowAnyException();
      assertThat(logs.events())
          .singleElement()
          .satisfies(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                    .isEqualTo("Could not write metric Lookup for countries");
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo("sink unavailable");
              });
    }
  }

  @Test
  void constructor_shouldRejectABlankNamespace_whenEmfIsEnabled() {
    // When & Then
    assertThatThrownBy(() -> new EmfMetricWriter(true, " ", () -> new MetricsLogger(environment)))
        .isInstanceOf(InvalidNamespaceException.class);
  }

  @Test
  void constructor_shouldAcceptABlankNamespace_whenEmfIsDisabled() {
    // When & Then
    assertThatCode(() -> new EmfMetricWriter(false, "", () -> new MetricsLogger(environment)))
        .doesNotThrowAnyException();
  }

  @Test
  void write_shouldEmitNothing_whenEmfIsDisabled() {
    var suppliedLoggers = new AtomicInteger();
    var disabled =
        new EmfMetricWriter(
            false,
            NAMESPACE,
            () -> {
              suppliedLoggers.incrementAndGet();
              return new MetricsLogger(environment);
            });

    disabled.write("Lookup", 1, Unit.COUNT, "LookupType", "countries");

    assertThat(disabled.isEnabled()).isFalse();
    assertThat(environment.documents()).isEmpty();
    assertThat(suppliedLoggers).hasValue(0);
  }
}
