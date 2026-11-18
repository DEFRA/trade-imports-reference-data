package uk.gov.defra.trade.imports.metrics;

/** The name and tag keys of every custom metric the service publishes. */
public final class MetricNames {

  public static final String EXTERNAL_CALL_DURATION = "external.call.duration";
  public static final String EXTERNAL_CALL_FAILURE = "external.call.failure";
  public static final String EXTERNAL_DEPENDENCY_AVAILABLE = "external.dependency.available";
  public static final String CACHE_HIT = "cache.hit";
  public static final String CACHE_MISS = "cache.miss";
  public static final String LOOKUP = "lookup";

  public static final String DEPENDENCY_TAG = "dependency";
  public static final String OPERATION_TAG = "operation";
  public static final String CACHE_TAG = "cache";
  public static final String LOOKUP_TYPE_TAG = "lookup.type";

  private MetricNames() {}
}
