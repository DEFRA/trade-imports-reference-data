package uk.gov.defra.trade.imports.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Counts each lookup the service is asked for as {@code lookup}, tagged {@code lookup.type}. */
@Component
@RequiredArgsConstructor
public class LookupMetrics {

  public static final String COUNTRIES = "countries";
  public static final String PORTS_OF_ENTRY = "ports-of-entry";
  public static final String COMMODITY_SEARCH = "commodity-search";
  public static final String SPECIES_CHECK = "species-check";

  private final MeterRegistry meterRegistry;

  public void recordLookup(String lookupType) {
    Counter.builder(MetricNames.LOOKUP)
        .tag(MetricNames.LOOKUP_TYPE_TAG, lookupType)
        .register(meterRegistry)
        .increment();
  }
}
