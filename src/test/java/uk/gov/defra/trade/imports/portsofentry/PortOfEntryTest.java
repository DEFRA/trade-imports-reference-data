package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;

class PortOfEntryTest {

  @Test
  void constructor_takesTheTypeFromTheMdmTraffic() {
    var port = new PortOfEntry(mdmPort("GBLHR", "Airport"));

    assertThat(port.getCode()).isEqualTo("GBLHR");
    assertThat(port.getName()).isEqualTo("Port GBLHR");
    assertThat(port.getType()).isEqualTo(PortType.AIRPORT);
  }

  @Test
  void constructor_warnsOnceWithThePortCodeWhenTheTrafficIsUnknown() {
    try (var logs = CapturingLogAppender.attachedTo(PortOfEntry.class)) {
      var first = new PortOfEntry(mdmPort("GBHOV", "Hovercraft"));
      var second = new PortOfEntry(mdmPort("GBHOV", "Hovercraft"));

      assertThat(first.getType()).isNull();
      assertThat(second.getType()).isNull();
      assertThat(logs.events())
          .singleElement()
          .satisfies(event -> assertThat(event.getFormattedMessage())
              .startsWith("MDM port GBHOV has traffic 'Hovercraft', which is not a known port type"));
    }
  }

  private static MdmPortOfEntry mdmPort(String code, String traffic) {
    return MdmPortOfEntry.builder().code(code).name("Port " + code).traffic(traffic).build();
  }
}
