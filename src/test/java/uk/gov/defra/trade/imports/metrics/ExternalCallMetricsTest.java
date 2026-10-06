package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

class ExternalCallMetricsTest {

  private static final String NAMESPACE = "trade-imports-reference-data";

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final ExternalCallMetrics metrics =
      new ExternalCallMetrics(true, NAMESPACE, () -> new MetricsLogger(environment));

  @Test
  void measure_shouldReturnTheResultAndEmitOneSuccessDocument() throws IOException {
    String result = metrics.measure(ExternalCall.MDM_GET_COUNTRIES, () -> "ok", r -> false);

    assertThat(result).isEqualTo("ok");
    assertThat(environment.documents()).hasSize(1);
    JsonNode document = environment.documents().getFirst();
    JsonNode directive = document.get("_aws").get("CloudWatchMetrics").get(0);
    assertThat(directive.get("Namespace").asText()).isEqualTo(NAMESPACE);
    assertThat(directive.get("Dimensions")).hasSize(1);
    assertThat(directive.get("Dimensions").get(0)).hasSize(2);
    assertThat(directive.get("Dimensions").get(0).get(0).asText()).isEqualTo("Dependency");
    assertThat(directive.get("Dimensions").get(0).get(1).asText()).isEqualTo("Operation");
    assertThat(directive.get("Metrics")).hasSize(2);
    assertThat(directive.get("Metrics").get(0).get("Name").asText())
        .isEqualTo("ExternalCallDuration");
    assertThat(directive.get("Metrics").get(0).get("Unit").asText()).isEqualTo("Milliseconds");
    assertThat(directive.get("Metrics").get(1).get("Name").asText())
        .isEqualTo("ExternalCallFailure");
    assertThat(directive.get("Metrics").get(1).get("Unit").asText()).isEqualTo("Count");
    assertThat(document.get("Dependency").asText()).isEqualTo("mdm");
    assertThat(document.get("Operation").asText()).isEqualTo("get-countries");
    assertThat(document.get("ExternalCallDuration").asDouble()).isGreaterThanOrEqualTo(0);
    assertThat(document.get("ExternalCallFailure").asInt()).isZero();
    assertThat(document.get("Outcome").asText()).isEqualTo("success");
    assertThat(document.get("Interface").asText()).isEqualTo("SYN-19");
    assertThat(document.has("LogGroup")).isFalse();
    assertThat(document.has("ServiceName")).isFalse();
    assertThat(document.has("ServiceType")).isFalse();
  }

  @Test
  void record_shouldEmitTheDurationInMilliseconds() {
    metrics.record(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(250), false);

    assertThat(environment.documents().getFirst().get("ExternalCallDuration").asDouble())
        .isEqualTo(250.0);
  }

  @Test
  void measure_shouldRecordAFailure_whenTheResultIsAFailure()throws IOException {
    metrics.measure(ExternalCall.MDM_GET_COUNTRIES, () -> "bad", r -> true);

    JsonNode document = environment.documents().getFirst();
    assertThat(document.get("ExternalCallFailure").asInt()).isEqualTo(1);
    assertThat(document.get("Outcome").asText()).isEqualTo("failure");
  }

  @Test
  void measure_shouldRethrowTheSameIOException_andRecordAFailure() {
    IOException failure = new IOException("connection reset");

    assertThatThrownBy(
            () ->
                metrics.measure(
                    ExternalCall.MDM_GET_PORTS_OF_ENTRY,
                    () -> {
                      throw failure;
                    },
                    r -> false))
        .isSameAs(failure);

    JsonNode document = environment.documents().getFirst();
    assertThat(document.get("ExternalCallFailure").asInt()).isEqualTo(1);
    assertThat(document.get("Outcome").asText()).isEqualTo("failure");
  }

  @Test
  void measure_shouldRethrowTheSameRuntimeException_andRecordAFailure() {
    IllegalStateException failure = new IllegalStateException("boom");

    assertThatThrownBy(
            () ->
                metrics.measure(
                    ExternalCall.TRADE_TOKEN,
                    () -> {
                      throw failure;
                    },
                    r -> false))
        .isSameAs(failure);

    assertThat(environment.documents().getFirst().get("ExternalCallFailure").asInt())
        .isEqualTo(1);
  }

  @Test
  void record_shouldNotThrow_whenTheSinkFails() {
    var failingEnvironment = new CapturingEmfEnvironment(true);
    var failingMetrics =
        new ExternalCallMetrics(true, NAMESPACE, () -> new MetricsLogger(failingEnvironment));

    assertThatCode(
            () -> failingMetrics.record(ExternalCall.TRADE_TOKEN, Duration.ofMillis(5), false))
        .doesNotThrowAnyException();
  }

  @Test
  void record_shouldEmitNoInterface_whenTheCallHasNone() {
    metrics.record(new ExternalCall("discovery", "lookup", null), Duration.ofMillis(5), false);

    assertThat(environment.documents().getFirst().has("Interface")).isFalse();
  }

  @Test
  void record_shouldEmitNothing_whenEmfIsDisabled() {
    var suppliedLoggers = new AtomicInteger();
    var disabled =
        new ExternalCallMetrics(
            false,
            NAMESPACE,
            () -> {
              suppliedLoggers.incrementAndGet();
              return new MetricsLogger(environment);
            });

    disabled.record(ExternalCall.TRADE_TOKEN, Duration.ofMillis(5), false);

    assertThat(environment.documents()).isEmpty();
    assertThat(suppliedLoggers).hasValue(0);
  }
}
