package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.loggedBy;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;

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
    var ports = new ArrayList<PortOfEntry>();

    var events =
        loggedBy(
            PortOfEntry.class,
            () -> {
              ports.add(new PortOfEntry(mdmPort("GBHOV", "Hovercraft")));
              ports.add(new PortOfEntry(mdmPort("GBHOV", "Hovercraft")));
            });

    assertThat(ports).allSatisfy(port -> assertThat(port.getType()).isNull());
    assertThat(events)
        .singleElement()
        .satisfies(event -> assertThat(event.getFormattedMessage())
            .startsWith("MDM port GBHOV has traffic 'Hovercraft', which is not a known port type"));
  }

  private static MdmPortOfEntry mdmPort(String code, String traffic) {
    return MdmPortOfEntry.builder().code(code).name("Port " + code).traffic(traffic).build();
  }
}
