package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PortTypeTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "Airport|AIRPORT",
      "PORT|SEAPORT",
      "' rail '|RAIL"
  })
  void fromTraffic_matchesIgnoringCaseAndSurroundingWhitespace(String traffic, PortType expected) {
    var type = PortType.fromTraffic(traffic);

    assertThat(type).contains(expected);
  }

  @Test
  void fromTraffic_findsNoTypeForNullTraffic() {
    var type = PortType.fromTraffic(null);

    assertThat(type).isEmpty();
  }

  @Test
  void fromTraffic_findsNoTypeForUnknownTraffic() {
    var type = PortType.fromTraffic("Hovercraft");

    assertThat(type).isEmpty();
  }
}
