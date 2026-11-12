package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DependencyAvailabilityMetricsTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final LastCallOutcomes outcomes = new LastCallOutcomes();

  @BeforeEach
  void registerTheGauges() {
    new DependencyAvailabilityMetrics(meterRegistry, outcomes);
  }

  @Test
  void gauge_shouldBeRegisteredForEveryDependency() {
    assertThat(meterRegistry.find("external.dependency.available").gauges())
        .extracting(gauge -> gauge.getId().getTag("dependency"))
        .containsExactlyInAnyOrder("mdm", "trade-token");
  }

  @Test
  void gauge_shouldBeNaN_whenTheDependencyHasNotBeenCalled() {
    assertThat(gauge("mdm").value()).isNaN();
  }

  @Test
  void gauge_shouldBeOne_whenTheLastCallSucceeded() {
    outcomes.recordSuccess("mdm");

    assertThat(gauge("mdm").value()).isEqualTo(1);
  }

  @Test
  void gauge_shouldBeOne_whenASuccessFollowsAFailure() {
    outcomes.recordFailure("mdm");
    outcomes.recordSuccess("mdm");

    assertThat(gauge("mdm").value()).isEqualTo(1);
  }

  @Test
  void gauge_shouldBeZeroForThatDependencyOnly_whenItsLastCallFailed() {
    outcomes.recordSuccess("mdm");
    outcomes.recordFailure("mdm");

    assertThat(gauge("mdm").value()).isZero();
    assertThat(gauge("trade-token").value()).isNaN();
  }

  private Gauge gauge(String dependency) {
    return meterRegistry.get("external.dependency.available").tag("dependency", dependency).gauge();
  }
}
