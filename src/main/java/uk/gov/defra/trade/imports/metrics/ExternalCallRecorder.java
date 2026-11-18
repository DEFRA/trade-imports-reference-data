package uk.gov.defra.trade.imports.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/**
 * Records each call to a dependency as the timer {@code external.call.duration} and the
 * distribution summary {@code external.call.failure} (1 for a failure, 0 for a success), both
 * tagged {@code dependency} and {@code operation}, and logs it with the ECS {@code event.*} fields.
 * A call that threw logs the throwable with the ECS {@code error.type} and {@code error.message}
 * fields; a call the dependency answered with an error status logs {@code
 * http.response.status_code}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalCallRecorder {

  private final MeterRegistry meterRegistry;

  /**
   * Records a call that succeeded.
   *
   * @param call the call made
   * @param elapsed how long the call took
   */
  public void recordSuccess(ExternalCall call, Duration elapsed) {
    recordMeters(call, elapsed, 0);
    callLine(Level.INFO, call, "success", elapsed)
        .log("External call {} success in {} ms", actionOf(call), elapsed.toMillis());
  }

  /**
   * Records a call that threw.
   *
   * @param call the call made
   * @param elapsed how long the call took
   * @param cause what it threw
   */
  public void recordFailure(ExternalCall call, Duration elapsed, Throwable cause) {
    recordMeters(call, elapsed, 1);
    callLine(Level.WARN, call, "failure", elapsed)
        .addKeyValue("error.type", cause.getClass().getName())
        .addKeyValue("error.message", cause.getMessage())
        .setCause(cause)
        .log("External call {} failure in {} ms", actionOf(call), elapsed.toMillis());
  }

  /**
   * Records a call the dependency answered with an error status.
   *
   * @param call the call made
   * @param elapsed how long the call took
   * @param status the HTTP status the dependency answered with
   */
  public void recordFailure(ExternalCall call, Duration elapsed, int status) {
    recordMeters(call, elapsed, 1);
    callLine(Level.WARN, call, "failure", elapsed)
        .addKeyValue("http.response.status_code", status)
        .log(
            "External call {} failure in {} ms with status {}",
            actionOf(call),
            elapsed.toMillis(),
            status);
  }

  private void recordMeters(ExternalCall call, Duration elapsed, int failure) {
    Tags tags =
        Tags.of(
            MetricNames.DEPENDENCY_TAG,
            call.dependency(),
            MetricNames.OPERATION_TAG,
            call.operation());
    Timer.builder(MetricNames.EXTERNAL_CALL_DURATION)
        .tags(tags)
        .register(meterRegistry)
        .record(elapsed);
    DistributionSummary.builder(MetricNames.EXTERNAL_CALL_FAILURE)
        .tags(tags)
        .register(meterRegistry)
        .record(failure);
  }

  private static LoggingEventBuilder callLine(
      Level level, ExternalCall call, String outcome, Duration elapsed) {
    return log.atLevel(level)
        .addKeyValue("event.category", "external-call")
        .addKeyValue("event.action", actionOf(call))
        .addKeyValue("event.outcome", outcome)
        .addKeyValue("event.duration", elapsed.toNanos());
  }

  private static String actionOf(ExternalCall call) {
    return call.dependency() + "/" + call.operation();
  }
}
