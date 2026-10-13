package uk.gov.defra.trade.imports.metrics;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Measures each call to a system outside the INS boundary and emits one CloudWatch Embedded Metric
 * Format document per call.
 *
 * <p>Each document is published to the service's namespace with the single dimension set
 * {@code [Dependency, Operation]}. It carries {@code ExternalCallDuration} in milliseconds and
 * {@code ExternalCallFailure} as 1 for a failed call and 0 for a successful one, plus the
 * {@code Outcome} and, where the call has one, the {@code Interface} properties. Recording never
 * breaks a call: a failure to record is logged as a warning and swallowed.
 *
 * <p>Each call is also logged, because EMF documents go to CloudWatch and cannot be searched in
 * OpenSearch. The line carries the ECS {@code event.*} fields CDP keeps and, where there is one,
 * the request's {@code trace.id}. It is written whether or not EMF is enabled.
 */
@Slf4j
@Component
public class ExternalCallMetrics {

  static final String DURATION_METRIC = "ExternalCallDuration";
  static final String FAILURE_METRIC = "ExternalCallFailure";
  static final String EVENT_CATEGORY = "external-call";
  private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

  private final boolean enabled;
  private final String namespace;
  private final Supplier<MetricsLogger> metricsLoggers;
  private final Map<String, Boolean> lastCallSucceededByDependency = new ConcurrentHashMap<>();

  @Autowired
  public ExternalCallMetrics(
      @Value("${aws.emf.enabled:true}") boolean enabled,
      @Value("${aws.emf.namespace}") String namespace) {
    this(enabled, namespace, MetricsLogger::new);
  }

  /**
   * Supplies each call's {@code MetricsLogger}, so a test can capture the EMF output.
   *
   * @param enabled whether to record at all
   * @param namespace the CloudWatch namespace
   * @param metricsLoggers supplies a fresh logger for each recorded call
   */
  public ExternalCallMetrics(
      boolean enabled, String namespace, Supplier<MetricsLogger> metricsLoggers) {
    this.enabled = enabled;
    this.namespace = namespace;
    this.metricsLoggers = metricsLoggers;
  }

  /** A call that may throw an {@link IOException}. */
  @FunctionalInterface
  public interface Action<T> {
    T call() throws IOException;
  }

  /**
   * Runs the action, records its duration and outcome, and returns its result.
   *
   * @param call the call being measured
   * @param action the call to run
   * @param isFailure decides whether a returned result counts as a failure
   * @param <T> the result type
   * @return the action's result
   * @throws IOException the action's own exception, rethrown unchanged after it is recorded
   */
  public <T> T measure(ExternalCall call, Action<T> action, Predicate<? super T> isFailure)
      throws IOException {
    long startedAt = System.nanoTime();
    T result;
    try {
      result = action.call();
    } catch (IOException | RuntimeException e) {
      recordCall(call, Duration.ofNanos(System.nanoTime() - startedAt), true);
      throw e;
    }
    recordCall(call, Duration.ofNanos(System.nanoTime() - startedAt), isFailure.test(result));
    return result;
  }

  /**
   * Emits one document for a finished call. Never throws.
   *
   * @param call the call that finished
   * @param elapsed how long it took
   * @param failed whether it failed
   */
  public void recordCall(ExternalCall call, Duration elapsed, boolean failed) {
    lastCallSucceededByDependency.put(call.dependency(), !failed);
    logCall(call, elapsed, failed);
    if (!enabled) {
      return;
    }
    try {
      var metricsLogger = metricsLoggers.get();
      metricsLogger.setNamespace(namespace);
      metricsLogger.setDimensions(
          DimensionSet.of("Dependency", call.dependency(), "Operation", call.operation()));
      metricsLogger.putMetric(
          DURATION_METRIC, elapsed.toNanos() / NANOS_PER_MILLISECOND, Unit.MILLISECONDS);
      metricsLogger.putMetric(FAILURE_METRIC, failed ? 1 : 0, Unit.COUNT);
      metricsLogger.putProperty("Outcome", failed ? "failure" : "success");
      if (call.interfaceId() != null) {
        metricsLogger.putProperty("Interface", call.interfaceId());
      }
      metricsLogger.flush();
    } catch (RuntimeException e) {
      log.warn(
          "Could not record external call {}/{}: {}",
          call.dependency(),
          call.operation(),
          e.getMessage());
    }
  }

  /**
   * Whether the most recent call to each dependency succeeded. A dependency that has not been
   * called since start-up is absent.
   *
   * @return an unmodifiable snapshot keyed by dependency name
   */
  public Map<String, Boolean> lastCallSucceededByDependency() {
    return Map.copyOf(lastCallSucceededByDependency);
  }

  private static void logCall(ExternalCall call, Duration elapsed, boolean failed) {
    String action = call.dependency() + "/" + call.operation();
    String outcome = failed ? "failure" : "success";
    try (var _ = MDC.putCloseable("event.category", EVENT_CATEGORY);
        var _ = MDC.putCloseable("event.action", action);
        var _ = MDC.putCloseable("event.outcome", outcome);
        var _ = MDC.putCloseable("event.duration", String.valueOf(elapsed.toNanos()))) {
      log.info("External call {} {} in {} ms", action, outcome, elapsed.toMillis());
    }
  }
}
