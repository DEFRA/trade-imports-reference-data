package uk.gov.defra.trade.imports.metrics;

import java.util.List;
import java.util.Objects;

/**
 * One call to a system outside the INS boundary, named as the external-call metric contract names
 * it.
 *
 * @param dependency the system called, for example {@code mdm}
 * @param operation the operation on that system, for example {@code get-countries}
 */
public record ExternalCall(String dependency, String operation) {

  public static final ExternalCall TRADE_TOKEN =
      new ExternalCall("trade-token", "client-credentials-token");
  public static final ExternalCall MDM_GET_COUNTRIES = new ExternalCall("mdm", "get-countries");
  public static final ExternalCall MDM_GET_PORTS_OF_ENTRY =
      new ExternalCall("mdm", "get-ports-of-entry");
  public static final ExternalCall MDM_SEARCH_PRODUCTS = new ExternalCall("mdm", "search-products");
  public static final ExternalCall MDM_GET_COMMODITIES = new ExternalCall("mdm", "get-commodities");
  public static final ExternalCall MDM_GET_COMMODITY_SPECIES =
      new ExternalCall("mdm", "get-commodity-species");

  /** Every dependency the service calls, each once. */
  public static final List<String> DEPENDENCIES = List.of("mdm", "trade-token");

  public ExternalCall {
    Objects.requireNonNull(dependency, "dependency must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }
}
