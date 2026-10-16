package uk.gov.defra.trade.imports.metrics;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.util.Validator;

/**
 * Writes one metric with one dimension as a CloudWatch Embedded Metric Format document in the
 * service's namespace.
 *
 * <p>The document carries only the dimension it is given, so a dashboard or alert can find the
 * metric by that dimension alone. Writing never breaks the caller: a failure to write is logged
 * as a warning and swallowed.
 */
@Slf4j
@Component
public class EmfMetricWriter {

  private final boolean enabled;
  private final String namespace;
  private final Supplier<MetricsLogger> metricsLoggers;

  @Autowired
  public EmfMetricWriter(
      @Value("${aws.emf.enabled:true}") boolean enabled,
      @Value("${aws.emf.namespace}") String namespace) {
    this(enabled, namespace, MetricsLogger::new);
  }

  /**
   * Supplies each document's {@code MetricsLogger}, so a test can capture the EMF output.
   *
   * @param enabled whether to write at all
   * @param namespace the CloudWatch namespace
   * @param metricsLoggers supplies a fresh logger for each document
   * @throws InvalidNamespaceException when enabled and the namespace is blank or invalid, so a bad
   *     namespace stops start-up instead of dropping every metric
   */
  public EmfMetricWriter(
      boolean enabled, String namespace, Supplier<MetricsLogger> metricsLoggers) {
    if (enabled) {
      Validator.validateNamespace(namespace);
    }
    this.enabled = enabled;
    this.namespace = namespace;
    this.metricsLoggers = metricsLoggers;
  }

  /**
   * Whether metrics are written, so a publisher can skip the work of gathering them.
   *
   * @return {@code true} when EMF is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Writes one document. Never throws.
   *
   * @param metric the metric name
   * @param value the metric value
   * @param unit the metric unit
   * @param dimension the dimension name
   * @param dimensionValue the dimension value
   */
  public void write(
      String metric, double value, Unit unit, String dimension, String dimensionValue) {
    if (!enabled) {
      return;
    }
    try {
      var metricsLogger = metricsLoggers.get();
      metricsLogger.setNamespace(namespace);
      metricsLogger.setDimensions(DimensionSet.of(dimension, dimensionValue));
      metricsLogger.putMetric(metric, value, unit);
      metricsLogger.flush();
    } catch (RuntimeException e) {
      log.warn("Could not write metric {} for {}", metric, dimensionValue, e);
    }
  }
}
