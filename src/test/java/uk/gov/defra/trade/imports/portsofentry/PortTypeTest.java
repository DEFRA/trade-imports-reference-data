package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class PortTypeTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "Airport|AIRPORT",
      "PORT|SEAPORT",
      "' rail '|RAIL"
  })
  void fromTraffic_matchesIgnoringCaseAndSurroundingWhitespace(String traffic, PortType expected) {
    // When
    var type = PortType.fromTraffic(traffic);

    // Then
    assertThat(type).contains(expected);
  }

  @Test
  void fromTraffic_findsNoTypeForNullTraffic() {
    // When
    var type = PortType.fromTraffic(null);

    // Then
    assertThat(type).isEmpty();
  }

  @Test
  void fromTraffic_reportsAnUnknownTrafficValueOnce(CapturedOutput output) {
    // When
    var first = PortType.fromTraffic("Hovercraft");
    var second = PortType.fromTraffic(" Hovercraft ");

    // Then
    assertThat(first).isEmpty();
    assertThat(second).isEmpty();
    assertThat(output.getOut().split("MDM port traffic 'Hovercraft' is not a known port type", -1)).hasSize(2);
  }
}
