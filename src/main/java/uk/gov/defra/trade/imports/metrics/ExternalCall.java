package uk.gov.defra.trade.imports.metrics;

import java.util.List;
import java.util.Objects;

/**
 * One call to a system outside the INS boundary.
 *
 * @param dependency the system called, for example {@code mdm}
 * @param operation the operation on that system, for example {@code get-countries}
 */
public record ExternalCall(String dependency, String operation) {

  private static final String MDM = "mdm";

  public static final ExternalCall TRADE_TOKEN =
      new ExternalCall("trade-token", "client-credentials-token");
  public static final ExternalCall MDM_GET_COUNTRIES = new ExternalCall(MDM, "get-countries");
  public static final ExternalCall MDM_GET_PORTS_OF_ENTRY =
      new ExternalCall(MDM, "get-ports-of-entry");
  public static final ExternalCall MDM_SEARCH_PRODUCTS = new ExternalCall(MDM, "search-products");
  public static final ExternalCall MDM_GET_COMMODITIES = new ExternalCall(MDM, "get-commodities");
  public static final ExternalCall MDM_GET_COMMODITY_SPECIES =
      new ExternalCall(MDM, "get-commodity-species");

  /** Every call the service makes to a dependency. */
  public static final List<ExternalCall> ALL =
      List.of(
          TRADE_TOKEN,
          MDM_GET_COUNTRIES,
          MDM_GET_PORTS_OF_ENTRY,
          MDM_SEARCH_PRODUCTS,
          MDM_GET_COMMODITIES,
          MDM_GET_COMMODITY_SPECIES);

  /** Every dependency the service calls, each once. */
  public static final List<String> DEPENDENCIES =
      ALL.stream().map(ExternalCall::dependency).distinct().toList();

  public ExternalCall {
    Objects.requireNonNull(dependency, "dependency must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }
}
