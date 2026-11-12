package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_COUNTRIES;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.TRADE_TOKEN;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.keyValues;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.loggedBy;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.durationCount;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.durationMillis;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.failureCount;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.failureTotal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalCallRecorderTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final ExternalCallRecorder recorder = new ExternalCallRecorder(meterRegistry);

  @Test
  void recordSuccess_recordsTheDurationAndAZeroFailure() {
    recorder.recordSuccess(MDM_GET_COUNTRIES, Duration.ofMillis(7));

    assertThat(durationCount(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(durationMillis(meterRegistry, "mdm", "get-countries")).isEqualTo(7);
    assertThat(failureCount(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(failureTotal(meterRegistry, "mdm", "get-countries")).isZero();
  }

  @Test
  void recordFailure_recordsTheDurationAndAFailureOfOne() {
    recorder.recordFailure(TRADE_TOKEN, Duration.ofMillis(3), new IOException("connection refused"));

    assertThat(durationCount(meterRegistry, "trade-token", "client-credentials-token"))
        .isEqualTo(1);
    assertThat(durationMillis(meterRegistry, "trade-token", "client-credentials-token"))
        .isEqualTo(3);
    assertThat(failureCount(meterRegistry, "trade-token", "client-credentials-token"))
        .isEqualTo(1);
    assertThat(failureTotal(meterRegistry, "trade-token", "client-credentials-token"))
        .isEqualTo(1);
  }

  @Test
  void recordSuccess_logsOneInfoLineWithTheEventFieldsAndANanosecondDuration() {
    List<ILoggingEvent> lines =
        loggedBy(
            ExternalCallRecorder.class,
            () -> recorder.recordSuccess(MDM_GET_COUNTRIES, Duration.ofMillis(2)));

    assertThat(lines).hasSize(1);
    ILoggingEvent line = lines.getFirst();
    assertThat(line.getLevel()).isEqualTo(Level.INFO);
    assertThat(line.getFormattedMessage())
        .isEqualTo("External call mdm/get-countries success in 2 ms");
    assertThat(keyValues(line))
        .containsEntry("event.category", "external-call")
        .containsEntry("event.action", "mdm/get-countries")
        .containsEntry("event.outcome", "success")
        .containsEntry("event.duration", 2_000_000L);
    assertThat(line.getThrowableProxy()).isNull();
  }

  @Test
  void recordFailure_logsOneWarnLineWithTheErrorFieldsAndTheThrowable_whenTheCallThrew() {
    IOException cause = new IOException("connection refused");

    List<ILoggingEvent> lines =
        loggedBy(
            ExternalCallRecorder.class,
            () -> recorder.recordFailure(MDM_GET_COUNTRIES, Duration.ofMillis(4), cause));

    assertThat(lines).hasSize(1);
    ILoggingEvent line = lines.getFirst();
    assertThat(line.getLevel()).isEqualTo(Level.WARN);
    assertThat(line.getFormattedMessage())
        .isEqualTo("External call mdm/get-countries failure in 4 ms");
    assertThat(keyValues(line))
        .containsEntry("event.action", "mdm/get-countries")
        .containsEntry("event.outcome", "failure")
        .containsEntry("error.type", "java.io.IOException")
        .containsEntry("error.message", "connection refused")
        .doesNotContainKey("http.response.status_code");
    assertThat(line.getThrowableProxy().getClassName()).isEqualTo("java.io.IOException");
    assertThat(line.getThrowableProxy().getMessage()).isEqualTo("connection refused");
  }

  @Test
  void recordFailure_logsOneWarnLineWithTheStatusCode_whenTheDependencyAnsweredWithAnError() {
    List<ILoggingEvent> lines =
        loggedBy(
            ExternalCallRecorder.class,
            () -> recorder.recordFailure(MDM_GET_COUNTRIES, Duration.ofMillis(5), 503));

    assertThat(lines).hasSize(1);
    ILoggingEvent line = lines.getFirst();
    assertThat(line.getLevel()).isEqualTo(Level.WARN);
    assertThat(line.getFormattedMessage())
        .isEqualTo("External call mdm/get-countries failure in 5 ms with status 503");
    assertThat(keyValues(line))
        .containsEntry("event.outcome", "failure")
        .containsEntry("http.response.status_code", 503)
        .doesNotContainKey("error.type");
    assertThat(line.getThrowableProxy()).isNull();
    assertThat(failureTotal(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
  }
}
