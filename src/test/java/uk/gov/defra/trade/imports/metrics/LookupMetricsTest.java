package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class LookupMetricsTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final LookupMetrics metrics = new LookupMetrics(meterRegistry);

  @Test
  void recordLookup_countsOneLookupTaggedWithItsType() {
    metrics.recordLookup(LookupMetrics.PORTS_OF_ENTRY);
    metrics.recordLookup(LookupMetrics.PORTS_OF_ENTRY);
    metrics.recordLookup(LookupMetrics.COUNTRIES);

    assertThat(count("ports-of-entry")).isEqualTo(2);
    assertThat(count("countries")).isEqualTo(1);
  }

  private double count(String lookupType) {
    return meterRegistry.get("lookup").tag("lookup.type", lookupType).counter().count();
  }
}
