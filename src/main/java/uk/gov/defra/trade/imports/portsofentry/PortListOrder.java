package uk.gov.defra.trade.imports.portsofentry;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The order port lists are served in.
 *
 * Names are only compared in this order, never changed.
 */
public final class PortListOrder {

  private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");

  /**
   * Airports, then seaports, then rail ports, then ports of unknown type.
   *
   * Each group runs A to Z by name ignoring letter case and reading non-breaking and doubled spaces as one,
   * then by code.
   */
  public static final Comparator<PortOfEntry> AIRPORTS_THEN_SEAPORTS_THEN_RAIL =
      Comparator.comparing(PortOfEntry::getType, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(port -> sortableName(port.getName()))
          .thenComparing(PortOfEntry::getCode, Comparator.nullsLast(Comparator.naturalOrder()));

  private PortListOrder() {
  }

  private static String sortableName(String name) {
    return name == null ? "" : WHITESPACE.matcher(name).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
  }
}
