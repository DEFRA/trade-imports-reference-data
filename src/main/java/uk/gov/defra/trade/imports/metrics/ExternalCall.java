package uk.gov.defra.trade.imports.metrics;

import java.util.Objects;

/**
 * One call to a system outside the INS boundary, named as the external-call metric contract names
 * it.
 *
 * @param dependency the system called, for example {@code mdm}
 * @param operation the operation on that system, for example {@code get-countries}
 * @param interfaceId the volumetrics SYN interface id, or {@code null} where there is none
 */
public record ExternalCall(String dependency, String operation, String interfaceId) {

  public static final ExternalCall TRADE_TOKEN =
      new ExternalCall("trade-token", "client-credentials-token", "SYN-19");
  public static final ExternalCall MDM_GET_COUNTRIES =
      new ExternalCall("mdm", "get-countries", "SYN-19");
  public static final ExternalCall MDM_GET_PORTS_OF_ENTRY =
      new ExternalCall("mdm", "get-ports-of-entry", "SYN-19");

  public ExternalCall {
    Objects.requireNonNull(dependency, "dependency must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
  }
}
