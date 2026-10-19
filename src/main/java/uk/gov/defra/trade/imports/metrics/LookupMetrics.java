package uk.gov.defra.trade.imports.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Counts each reference-data lookup the service is asked for, as {@code Lookup} with the single
 * dimension {@code LookupType}.
 */
@Component
@RequiredArgsConstructor
public class LookupMetrics {

  public static final String COUNTRIES = "countries";
  public static final String PORTS_OF_ENTRY = "ports-of-entry";
  public static final String COMMODITY_SEARCH = "commodity-search";
  public static final String SPECIES_CHECK = "species-check";
  static final String LOOKUP_METRIC = "Lookup";
  static final String LOOKUP_TYPE_DIMENSION = "LookupType";

  private final EmfMetricWriter emfMetricWriter;

  /**
   * Counts one lookup. Never throws.
   *
   * @param lookupType the kind of reference data asked for
   */
  public void recordLookup(String lookupType) {
    emfMetricWriter.write(LOOKUP_METRIC, 1, Unit.COUNT, LOOKUP_TYPE_DIMENSION, lookupType);
  }
}
