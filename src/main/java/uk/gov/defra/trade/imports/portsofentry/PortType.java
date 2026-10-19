package uk.gov.defra.trade.imports.portsofentry;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The kind of a port as MDM's traffic field records it.
 */
public enum PortType {
  AIRPORT("airport", "Airport"),
  SEAPORT("seaport", "Port"),
  RAIL("rail", "Rail");

  private final String value;
  private final String mdmTraffic;

  PortType(String value, String mdmTraffic) {
    this.value = value;
    this.mdmTraffic = mdmTraffic;
  }

  /**
   * The value the API writes for this type.
   *
   * @return the lower-case type name
   */
  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * Finds the port type for an MDM traffic value, ignoring letter case and surrounding whitespace.
   *
   * @param traffic the traffic value MDM gives a port, which may be null
   * @return the matching type, or empty when the traffic is null or not recognised
   */
  public static Optional<PortType> fromTraffic(String traffic) {
    String stripped = traffic == null ? "" : traffic.strip();
    return Stream.of(values())
        .filter(candidate -> candidate.mdmTraffic.equalsIgnoreCase(stripped))
        .findFirst();
  }
}
