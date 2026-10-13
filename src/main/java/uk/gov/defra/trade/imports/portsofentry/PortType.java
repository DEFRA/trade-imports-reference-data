package uk.gov.defra.trade.imports.portsofentry;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * The kind of a port as MDM's traffic field records it.
 *
 * Declaration order is the order port lists are grouped in: airports, then seaports, then rail ports.
 */
@Slf4j
public enum PortType {
  AIRPORT("airport", "Airport"),
  SEAPORT("seaport", "Port"),
  RAIL("rail", "Rail");

  private static final Set<String> REPORTED_UNKNOWN_TRAFFIC = ConcurrentHashMap.newKeySet();

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
   * A value that matches no type is logged once per distinct value, so a new MDM traffic value is seen rather
   * than quietly sorting its ports to the end of every list.
   *
   * @param traffic the traffic value MDM gives a port, which may be null
   * @return the matching type, or empty when the traffic is null or not recognised
   */
  public static Optional<PortType> fromTraffic(String traffic) {
    String stripped = traffic == null ? "" : traffic.strip();
    Optional<PortType> type = Stream.of(values())
        .filter(candidate -> candidate.mdmTraffic.equalsIgnoreCase(stripped))
        .findFirst();
    if (type.isEmpty() && REPORTED_UNKNOWN_TRAFFIC.add(stripped)) {
      log.warn("MDM port traffic '{}' is not a known port type; its ports are listed last with no type", stripped);
    }
    return type;
  }
}
