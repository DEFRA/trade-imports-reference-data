package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;

@ExtendWith(OutputCaptureExtension.class)
class PortOfEntryTest {

  @Test
  void constructor_takesTheTypeFromTheMdmTraffic() {
    var port = new PortOfEntry(mdmPort("GBLHR", "Airport"));

    assertThat(port.getCode()).isEqualTo("GBLHR");
    assertThat(port.getName()).isEqualTo("Port GBLHR");
    assertThat(port.getType()).isEqualTo(PortType.AIRPORT);
  }

  @Test
  void constructor_warnsOnceWithThePortCodeWhenTheTrafficIsUnknown(CapturedOutput output) {
    var first = new PortOfEntry(mdmPort("GBHOV", "Hovercraft"));
    var second = new PortOfEntry(mdmPort("GBHOV", "Hovercraft"));

    assertThat(first.getType()).isNull();
    assertThat(second.getType()).isNull();
    assertThat(output.getOut().split("MDM port GBHOV has traffic 'Hovercraft', which is not a known port type", -1))
        .hasSize(2);
  }

  private static MdmPortOfEntry mdmPort(String code, String traffic) {
    return MdmPortOfEntry.builder().code(code).name("Port " + code).traffic(traffic).build();
  }
}
